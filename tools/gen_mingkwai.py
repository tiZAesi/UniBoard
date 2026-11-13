#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「明快键盘」用的上下形索引。

明快中文打字机（林语堂，1946）靠「取字的上形与下形」两键定位汉字：
敲两个形状键，机器上方的窗口列出少数候选，再按号选取。

本项目没有那台机器的机内字表，改用公开字形数据近似还原这套思路：
  · 首形 = 字的部首（绝大部分汉字的上/左部件就是部首）
  · 尾形 = 字的末部件（由 IDS 部件分解串取最后一个部件）
两者都归并进同一套「形状键」，于是两键之后候选通常只剩个位数。

输入数据（不随仓库分发，下载一次即可复现）：
    curl -sL -o dict.txt \
      https://cdn.jsdelivr.net/gh/skishore/makemeahanzi@master/dictionary.txt
    python3 tools/gen_mingkwai.py dict.txt

产出：
    app/src/main/assets/lexicon/mingkwai_keys.txt   形状键表
    app/src/main/assets/lexicon/mingkwai_index.txt  两键索引（按字频排序）
"""
import json, sys, os, collections

OPS = set("⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿻？?")
MAX_KEYS = 29          # 形状键数量上限（不含「其他」），加「其他」正好 30 键：6 列 × 5 行

# 形状键：键名 -> 该形状的常见写法（含繁体、偏旁变体）
CLASSES = [
    ('水', '氵水氺'), ('草', '艹艸'), ('口', '口囗'), ('手', '扌手才'), ('木', '木'),
    ('人', '亻人入八'), ('金', '钅釒金'), ('心', '忄心'), ('虫', '虫'), ('月', '⺼月'),
    ('竹', '⺮竹'), ('言', '讠言訁'), ('土', '土士'), ('糸', '纟糹糸'), ('走', '辶辵廴走'),
    ('女', '女'), ('病', '疒'), ('火', '火灬'), ('足', '足⻊'), ('石', '石'),
    ('王', '王玉'), ('山', '山'), ('日', '日曰'), ('犬', '犭犬'), ('邑', '阝卩'),
    ('宀', '宀冖穴'), ('目', '目'), ('衣', '衤衣'), ('鱼', '鱼魚'), ('禾', '禾'),
    ('门', '门門'), ('车', '车車'), ('食', '饣飠食'), ('示', '礻示'), ('马', '马馬'),
    ('鸟', '鸟鳥'), ('刀', '刂刀'), ('力', '力'), ('大', '大'), ('十', '十'),
    ('页', '页頁'), ('贝', '贝貝'), ('攵', '攵夂'), ('又', '又'), ('皿', '皿'),
    ('雨', '雨'), ('弓', '弓'), ('白', '白'), ('立', '立'), ('米', '米'),
    ('田', '田'), ('巾', '巾'), ('子', '子'), ('工', '工'), ('方', '方'),
    ('戈', '戈'), ('隹', '隹'), ('户', '户'), ('尸', '尸'), ('彳', '彳'),
    ('欠', '欠'), ('殳', '殳'), ('寸', '寸'), ('几', '几'), ('干', '干'),
]

ALIAS = {}
for name, chars in CLASSES:
    for c in chars:
        ALIAS.setdefault(c, name)

OTHER = '其他'


def load_dict(path):
    rows, decom = [], {}
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                d = json.loads(line)
            except Exception:
                continue
            ch = d.get('character') or ''
            if len(ch) != 1 or not ('\u4e00' <= ch <= '\u9fff'):
                continue
            dec = d.get('decomposition') or ''
            decom[ch] = dec
            rows.append((ch, d.get('radical') or '', dec))
    return rows, decom


def make_classifier(decom):
    def cls(comp, deep=0):
        if comp in ALIAS:
            return ALIAS[comp]
        if deep < 2:
            subs = [c for c in decom.get(comp, '') if c not in OPS]
            if subs:
                return cls(subs[0], deep + 1)
        return OTHER
    return cls


def load_char_freq(lexicon):
    """从内置拼音词库统计单字频次，用来给候选排序"""
    freq = collections.Counter()
    if not os.path.exists(lexicon):
        return freq
    with open(lexicon, encoding='utf-8') as f:
        for line in f:
            tab = line.find('\t')
            if tab <= 0:
                continue
            for item in line[tab + 1:].strip().split(','):
                sp = item.rfind(':')
                if sp <= 0:
                    continue
                w = item[:sp]
                try:
                    n = int(item[sp + 1:])
                except ValueError:
                    continue
                if len(w) == 1:
                    freq[w] += n
                elif len(w) <= 4:
                    for c in w:
                        freq[c] += n // len(w)
    return freq


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_dir = os.path.join(root, 'app', 'src', 'main', 'assets', 'lexicon')
    lexicon = os.path.join(out_dir, 'zh_pinyin.txt')

    rows, decom = load_dict(sys.argv[1])
    cls = make_classifier(decom)

    pairs = []
    usage = collections.Counter()
    for ch, rad, dec in rows:
        subs = [c for c in dec if c not in OPS]
        a = cls(rad) if rad else OTHER
        b = cls(subs[-1]) if subs else OTHER
        pairs.append((ch, a, b))
        usage[a] += 1
        usage[b] += 1

    # 形状键：按使用频次取前 MAX_KEYS 个，其余归入「其他」
    ranked = [name for name, _ in usage.most_common() if name != OTHER]
    keys = ranked[:MAX_KEYS]
    if OTHER in usage:
        keys.append(OTHER)
    key_id = {name: i for i, name in enumerate(keys)}

    freq = load_char_freq(lexicon)
    buckets = collections.defaultdict(list)
    for ch, a, b in pairs:
        buckets[(key_id.get(a, key_id[OTHER]), key_id.get(b, key_id[OTHER]))].append(ch)

    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, 'mingkwai_keys.txt'), 'w', encoding='utf-8') as f:
        for name in keys:
            members = next((c for n, c in CLASSES if n == name), '')
            f.write(f"{name}\t{members}\n")

    total = 0
    with open(os.path.join(out_dir, 'mingkwai_index.txt'), 'w', encoding='utf-8') as f:
        for (i, j), chars in sorted(buckets.items()):
            chars.sort(key=lambda c: -freq.get(c, 0))
            payload = ','.join(f"{c}:{freq.get(c, 0) + 1}" for c in chars)
            f.write(f"{i}\t{j}\t{payload}\n")
            total += len(chars)

    sizes = sorted((len(v) for v in buckets.values()), reverse=True)
    print(f"形状键 {len(keys)} 个，桶 {len(buckets)} 个，汉字 {total} 个")
    print(f"最大桶 {sizes[0]}，中位数 {sizes[len(sizes)//2]}，平均 {sum(sizes)/len(sizes):.1f}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
