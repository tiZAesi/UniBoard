#!/usr/bin/env python3
# UniBoard 日文假名 -> 汉字 变换词典生成脚本
# Copyright (C) 2026 UniBoard contributors
# 本脚本是自由软件，依 GPL-3.0-or-later 授权。
#
# 数据来源（均为 CC-BY-SA 4.0，scriptin/jmdict-simplified 发布的 JMdict / KANJIDIC2 简化版）：
#   jmdict-eng-common-<rel>.json.tgz  —— 常用词条（主数据）
#   kanjidic2-en-<rel>.json.tgz       —— 单字读音（补充单字覆盖）
#   jmdict-eng-<rel>.json.tgz         —— 全量词条（仅用于覆盖扩展，见 FREQ_LIMIT）
#
# 说明：3.6.2 的简化版数据已经去掉了 news1/ichi1/nf01… 这类频率标签，
# 常用度只剩「逐写法的 common 布尔」。因此 base_cost() 先按标签规则走，
# 标签缺失时把 common 布尔按同样四档映射（常用读音 100 / 有常用写法 200 /
# 其余 900）；这样无论源数据是 v2 还是 3.x 格式，cost 语义都一致。
# 仅靠常用词条只有约 1.9 万读音、gzip 约 0.25 MB，明显偏小，
# 所以再用全量词条里「表记汉字全在报纸字频前 FREQ_LIMIT 名」的词条补覆盖。
#
# 用法：
#   python3 tools/gen_ja_dict.py              # 重建资源文件
#   python3 tools/gen_ja_dict.py --verify     # 重建后用内置解析器回读并打印样例候选
#
# 源数据缓存在 /tmp/ja_build/（可用环境变量 JA_BUILD_CACHE 覆盖），避开仓库，
# 缓存可复用时不会再次下载。输出 gzip 固定 mtime=0、键序与排序确定，故可重复。
#
# 输出格式（Kotlin 端解析约定，逐字一致）：
#   <reading>\t<surface>:<cost>,<surface>:<cost>,...
#   reading 只含平假名 U+3041-U+3096 与长音符 U+30FC；每行最多 8 个候选，
#   按 cost 升序、同 cost 按表记 Unicode 码位升序；行按 reading 码位升序；
#   无 BOM、无空行结尾。cost 越小越常用。

import gzip
import io
import json
import os
import sys
import tarfile
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_PATH = os.path.join(ROOT, "app", "src", "main", "assets", "lexicon", "ja_kanji.lex")

CACHE_DIR = os.environ.get("JA_BUILD_CACHE", "/tmp/ja_build")
# 固定发布版本，保证可重复构建
RELEASE = "3.6.2+20261109214828"
RELEASE_TAG = RELEASE.replace("+", "%2B")
DL_BASE = "https://github.com/scriptin/jmdict-simplified/releases/download/" + RELEASE_TAG
API_ASSET = "https://api.github.com/repos/scriptin/jmdict-simplified/releases/assets/%d"

# key, 文件名, GitHub asset id, 期望字节数
SOURCES = (
    ("jmdict-common", "jmdict-eng-common-%s.json.tgz" % RELEASE, 729104684, 1445476),
    ("kanjidic2", "kanjidic2-en-%s.json.tgz" % RELEASE, 729104774, 1253440),
    ("jmdict-full", "jmdict-eng-%s.json.tgz" % RELEASE, 729104677, 11543003),
)

# 覆盖率上限：同一 reading 最多保留的候选数
MAX_CANDIDATES = 8

# KANJIDIC2 报纸字频表大小（新闻频率 1..2501）
FREQ_TABLE_SIZE = 2501

# 全量词条扩展时使用的字频筛选：表记里每个汉字都必须出现在 KANJIDIC2 的
# 报纸字频表前 FREQ_LIMIT 名内（该表共 2501 字）。常用词条不受此限制。
FREQ_LIMIT = 1000

# 平假名 / 片假名 / 长音符
HIRA_LO, HIRA_HI = 0x3041, 0x3096
KATA_LO, KATA_HI = 0x30A1, 0x30F6
PROLONG = 0x30FC
# 汉字区间：基本区 + 扩展 A（扩展 B 及以后不收，避免体积膨胀）
KANJI_RANGES = ((0x4E00, 0x9FFF), (0x3400, 0x4DBF))

# 常用度标签（v2 格式的 JMdict 才有；3.x 简化为逐写法 common 布尔）
RANK1_TAGS = {"news1", "ichi1", "spec1", "gai1"} | {"nf%02d" % i for i in range(1, 25)}
RANK2_TAGS = {"news2", "ichi2", "spec2", "gai2"}
# 罕见 / 不规则 / 旧字 / 检索用写法
RARE_TAGS = {"rK", "iK", "oK", "sK"}
# 表记里不允许出现的字符（格式分隔符）
FORBIDDEN = set("\t\n\r,:，：")


def is_kanji(ch):
    o = ord(ch)
    return any(lo <= o <= hi for lo, hi in KANJI_RANGES)


def has_kanji(text):
    return any(is_kanji(c) for c in text)


def kana_to_hiragana(text):
    """片假名转平假名；长音符保留；含其他字符返回 None。"""
    out = []
    for ch in text:
        o = ord(ch)
        if KATA_LO <= o <= KATA_HI:
            out.append(chr(o - 0x60))
        elif HIRA_LO <= o <= HIRA_HI or o == PROLONG:
            out.append(ch)
        else:
            return None
    return "".join(out)


def normalize_reading(text):
    """只接受纯平假名（含长音符）的读音。"""
    if not text:
        return None
    for ch in text:
        o = ord(ch)
        if not (HIRA_LO <= o <= HIRA_HI or o == PROLONG):
            return None
    return text


def surface_ok(text):
    if not text or not has_kanji(text):
        return False
    if any(c in FORBIDDEN or c.isspace() for c in text):
        return False
    return True


# --------------------------------------------------------------------------
# 下载与缓存
# --------------------------------------------------------------------------

def _read_token():
    """GitHub token 仅用于下载 API；不打印、不落盘到仓库。"""
    for key in ("GITHUB_TOKEN", "GH_TOKEN"):
        tok = os.environ.get(key)
        if tok:
            return tok.strip()
    try:
        with open("/root/.uniboard_pat", encoding="utf-8") as f:
            return f.read().strip()
    except OSError:
        return ""


def _fetch(url, dest, token="", timeout=60):
    req = urllib.request.Request(url, headers={"User-Agent": "uniboard-gen-ja-dict"})
    if token:
        req.add_header("Authorization", "Bearer " + token)
        req.add_header("Accept", "application/octet-stream")
    with urllib.request.urlopen(req, timeout=timeout) as resp, open(dest, "wb") as fh:
        while True:
            chunk = resp.read(1 << 16)
            if not chunk:
                break
            fh.write(chunk)


def _cache_usable(path, expect_size):
    try:
        if os.path.getsize(path) != expect_size:
            return False
        with tarfile.open(path, "r:gz") as tar:
            for member in tar:
                if member.name.endswith(".json"):
                    return True
        return False
    except (OSError, tarfile.TarError):
        return False


def ensure_source(key, filename, asset_id, expect_size):
    os.makedirs(CACHE_DIR, exist_ok=True)
    path = os.path.join(CACHE_DIR, key + ".tgz")
    if _cache_usable(path, expect_size):
        return path
    if os.path.exists(path):
        os.remove(path)
    tmp = path + ".part"
    errors = []
    # 1) 先试直连 release 下载
    try:
        _fetch(DL_BASE + "/" + urllib.parse.quote(filename), tmp)
        if os.path.getsize(tmp) == expect_size:
            os.replace(tmp, path)
            return path
        errors.append("direct: size mismatch")
    except Exception as exc:  # noqa: BLE001 - 网络路径失败时回落到 API
        errors.append("direct: %s" % exc)
    # 2) 回落到 GitHub API asset 端点
    token = _read_token()
    for attempt in range(3):
        try:
            if os.path.exists(tmp):
                os.remove(tmp)
            _fetch(API_ASSET % asset_id, tmp, token=token, timeout=120)
            if os.path.getsize(tmp) == expect_size:
                os.replace(tmp, path)
                return path
            errors.append("api: size mismatch")
        except Exception as exc:  # noqa: BLE001
            errors.append("api: %s" % exc)
        time.sleep(1 + attempt)
    if os.path.exists(tmp):
        os.remove(tmp)
    raise SystemExit("无法获取 %s：%s" % (filename, "; ".join(errors)))


def load_json(path):
    with tarfile.open(path, "r:gz") as tar:
        for member in tar:
            if member.name.endswith(".json"):
                fh = tar.extractfile(member)
                return json.load(io.TextIOWrapper(fh, encoding="utf-8"))
    raise SystemExit("压缩包内没有 JSON：%s" % path)


# --------------------------------------------------------------------------
# 词典构建
# --------------------------------------------------------------------------

def base_cost(kana, kanji_list, word):
    """按词条常用度决定基础 cost。"""
    tags = set(kana.get("tags") or ())
    if tags & RANK1_TAGS:
        return 100
    if tags & RANK2_TAGS:
        return 200
    if word.get("common"):  # v2 格式：{"word": {"news1": 4}, ...}
        return 300
    # 3.x 简化格式把频率信息压缩成逐个写法的 common 布尔，
    # 这里按其语义映射到同样的四档：
    #   常用读音 -> 100；词条有常用写法 -> 200；否则 -> 900
    if kana.get("common"):
        return 100
    if any(k.get("common") for k in kanji_list):
        return 200
    # 非常用词条给一个明显高的代价：日文输入法对「读音碰巧等于某个生僻词」的场景
    # 应该优先保留假名（助词、外来语），而不是硬转成 廃位 / 伝写 这类词。
    # 它们在候选列表里仍然出现（编辑器侧会补「其它写法」），只是不会抢第一位。
    return 2400


def add_pair(index, reading, surface, cost):
    bucket = index.setdefault(reading, {})
    old = bucket.get(surface)
    if old is None or cost < old:
        bucket[surface] = cost


def build_from_jmdict(words, index, keeper=None):
    """把 JMdict 词条并入 index。keeper 用于全量词条的覆盖扩展筛选。"""
    for word in words:
        kanji_list = word.get("kanji") or []
        if not kanji_list:
            continue
        surfaces = []
        for rank, entry in enumerate(kanji_list):
            text = entry.get("text") or ""
            if not surface_ok(text):
                continue
            surfaces.append((rank, entry, text))
        if not surfaces:
            continue
        if keeper is not None and not any(keeper(text) for _, _, text in surfaces):
            continue
        for kana in word.get("kana") or []:
            reading = kana_to_hiragana(kana.get("text") or "")
            if reading is None:
                continue
            base = base_cost(kana, kanji_list, word)
            for rank, entry, text in surfaces:
                cost = base + rank * 40
                if set(entry.get("tags") or ()) & RARE_TAGS:
                    cost += 4000
                if len(text) == 1:
                    if len(reading) > 1:
                        # 同一个读音下，「今日」这种多字表记通常比单字表记更精确
                        # （京 / 卿 / 経 都能读 きょう），小罚一点让它们排在后面。
                        cost += 30
                    if len(reading) == 1:
                        # 单假名读音的单字段（は→羽、に→荷）太容易把助词抢成汉字，
                        # 只有这种情况才重罚；「やま→山」这种两假名读音不该受罚，
                        # 否则永远转不出汉字。
                        cost += 1500
                elif len(text) >= 6:
                    cost += 600
                # 汉字 + 送假名混排（食べる、愛し）统一小罚一点：
                # 同样读音下优先纯汉字词（橋 / 箸），但不会让 食べる 输给假名。
                if any(HIRA_LO <= ord(ch) <= HIRA_HI for ch in text):
                    cost += 300
                add_pair(index, reading, text, cost)


def build_from_kanjidic(characters, index):
    """单字补充：JMdict 里已存在的 (reading, surface) 组合不覆盖。

    只收 U+4E00-U+9FFF 且带常用/教育/人名用标记或报纸字频的汉字，
    扩展区的生僻字和无人使用的异体字不进词典（否则只是体积和噪声）。
    排名惩罚：有报纸字频就用字频名次，否则排在字频表之后（按源文件顺序）。
    """
    unranked = 0
    for char in characters:
        literal = char.get("literal") or ""
        if len(literal) != 1 or not (0x4E00 <= ord(literal) <= 0x9FFF):
            continue  # 扩展 B 区及以后的生僻字不收
        misc = char.get("misc") or {}
        rank = misc.get("frequency")
        if not rank and not (misc.get("grade") or misc.get("jlptLevel")):
            continue
        if not rank:
            unranked += 1
            rank = FREQ_TABLE_SIZE + unranked
        rank -= 1  # 从 0 起
        reading_meaning = char.get("readingMeaning") or {}
        for group in reading_meaning.get("groups") or []:
            for item in group.get("readings") or []:
                kind = item.get("type")
                value = (item.get("value") or "").strip("-")
                if kind == "ja_kun":
                    cost = 3000
                elif kind == "ja_on":
                    value = kana_to_hiragana(value)
                    if value is None:
                        continue
                    cost = 4000
                else:
                    continue
                reading = normalize_reading(value)
                if reading is None:
                    continue
                bucket = index.get(reading)
                if bucket is not None and literal in bucket:
                    continue  # JMdict 优先
                add_pair(index, reading, literal, cost + rank)


def make_keeper(freq_rank):
    def keeper(text):
        for ch in text:
            if is_kanji(ch):
                rank = freq_rank.get(ch)
                if rank is None or rank > FREQ_LIMIT:
                    return False
        return True

    return keeper


def render(index):
    """按格式渲染成 UTF-8 文本（不含 BOM，末尾单个换行）。"""
    lines = []
    for reading in sorted(index):
        bucket = index[reading]
        if not bucket:
            continue
        cands = sorted(bucket.items(), key=lambda kv: (kv[1], kv[0]))[:MAX_CANDIDATES]
        lines.append(reading + "\t" + ",".join("%s:%d" % (s, c) for s, c in cands))
    text = "\n".join(lines) + "\n"
    return text, lines


def gzip_bytes(text):
    buf = io.BytesIO()
    # mtime=0 + 不写文件名：同样的输入得到字节级相同的输出
    with gzip.GzipFile(fileobj=buf, mode="wb", compresslevel=9, mtime=0) as gz:
        gz.write(text.encode("utf-8"))
    return buf.getvalue()


# --------------------------------------------------------------------------
# 内置解析器（复现 Kotlin 端读法，用于自检）
# --------------------------------------------------------------------------

def parse_lexicon(blob):
    table = {}
    with gzip.GzipFile(fileobj=io.BytesIO(blob)) as gz:
        data = gz.read().decode("utf-8")
    for line in data.split("\n"):
        if not line:
            continue
        reading, _, cands = line.partition("\t")
        items = []
        for chunk in cands.split(","):
            surface, _, cost = chunk.rpartition(":")
            items.append((surface, int(cost)))
        table[reading] = items
    return table


SAMPLES = (
    "にほん",
    "がっこう",
    "とうきょう",
    "ありがとう",
    "たべる",
    "やま",
    "きょう",
    "でんしゃ",
    "うつくしい",
    "かいぎしつ",
)


def verify(blob, lines):
    table = parse_lexicon(blob)
    print("--- 样例候选（读法与 Kotlin 端一致）")
    for reading in SAMPLES:
        items = table.get(reading, [])
        body = ", ".join("%s:%d" % (s, c) for s, c in items) or "(空)"
        print("%s\t%s" % (reading, body))
    print("--- 格式自检")
    problems = []
    prev = None
    pairs = 0
    for line in lines:
        reading, sep, cands = line.partition("\t")
        if not sep:
            problems.append("缺少制表符: %r" % line)
            continue
        if normalize_reading(reading) != reading:
            problems.append("读音非法: %r" % reading)
        if prev is not None and reading <= prev:
            problems.append("行序错误: %r" % reading)
        prev = reading
        chunks = cands.split(",")
        if not 1 <= len(chunks) <= MAX_CANDIDATES:
            problems.append("候选数越界: %r" % reading)
        last = None
        for chunk in chunks:
            surface, _, cost = chunk.rpartition(":")
            try:
                value = int(cost)
            except ValueError:
                problems.append("cost 非法: %r" % chunk)
                continue
            if not 1 <= value <= 999999:
                problems.append("cost 越界: %r" % chunk)
            if not has_kanji(surface) or any(c in FORBIDDEN for c in surface):
                problems.append("表记非法: %r" % surface)
            if last is not None and (value, surface) < last:
                problems.append("候选未排序: %r" % chunk)
            last = (value, surface)
            pairs += 1
    print("problems: %d" % len(problems))
    for item in problems[:10]:
        print("  " + item)
    return pairs, problems


def main(argv):
    index = {}

    common_path = ensure_source(*SOURCES[0])
    kanjidic_path = ensure_source(*SOURCES[1])
    full_path = ensure_source(*SOURCES[2])

    common = load_json(common_path)
    build_from_jmdict(common.get("words") or [], index)

    characters = load_json(kanjidic_path).get("characters") or []
    freq_rank = {}
    for char in characters:
        rank = (char.get("misc") or {}).get("frequency")
        if rank:
            freq_rank[char.get("literal")] = rank

    # 覆盖扩展：全量词条里字频够常用的词条（常用词条已在上一步全量收下）
    keeper = make_keeper(freq_rank)
    build_from_jmdict(load_json(full_path).get("words") or [], index, keeper=keeper)

    # 单字补充放在最后：JMdict 已有的组合不覆盖
    build_from_kanjidic(characters, index)

    text, lines = render(index)
    blob = gzip_bytes(text)

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    tmp = OUT_PATH + ".tmp"
    with open(tmp, "wb") as fh:
        fh.write(blob)
    os.replace(tmp, OUT_PATH)

    pairs = sum(len(v) for v in index.values())
    print("输出: %s" % os.path.relpath(OUT_PATH, ROOT))
    print("gzip 大小: %d 字节 (%.2f MB)" % (len(blob), len(blob) / 1048576.0))
    print("明文大小: %d 字节" % len(text.encode("utf-8")))
    print("总行数: %d" % len(lines))
    print("(reading,surface) 对数: %d" % pairs)
    print("平均每行候选数: %.2f" % (pairs / float(len(lines)) if lines else 0.0))

    if "--verify" in argv:
        verify(blob, lines)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
