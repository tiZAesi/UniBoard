#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「笔画输入」用的笔画索引。

数据来自 hanzi-writer-data（每字给出各笔的中线折线），按折线走向判定五种基本笔画：
    1 横  2 竖  3 撇  4 点/捺  5 折
判定规则（够用即可，笔画输入的容错本来就很宽）：
  · 折线累计转角超过 60° → 折
  · 否则看首尾位移：横向为主 → 横；纵向为主 → 竖；
    右下 → 捺；左下 → 撇；向上 → 横（提）或竖（钩）

用法：
    npm pack hanzi-writer-data  # 或直接下 tarball
    tar xzf hanzi-writer-data-*.tgz
    python3 tools/gen_stroke.py package
产出：app/src/main/assets/lexicon/stroke_index.txt
"""
import json, os, sys, collections, math

MAX_STROKES = 6


def angle(a, b, c):
    """折线转角（度）"""
    v1 = (b[0] - a[0], b[1] - a[1])
    v2 = (c[0] - b[0], c[1] - b[1])
    n1 = math.hypot(*v1)
    n2 = math.hypot(*v2)
    if n1 < 1e-6 or n2 < 1e-6:
        return 0.0
    cos = (v1[0] * v2[0] + v1[1] * v2[1]) / (n1 * n2)
    cos = max(-1.0, min(1.0, cos))
    return math.degrees(math.acos(cos))


def stroke_type(points):
    """按中线走向判定笔画类型：1 横 2 竖 3 撇 4 点/捺 5 折"""
    if not points or len(points) < 2:
        return 4

    segs = []
    for i in range(len(points) - 1):
        dx = points[i + 1][0] - points[i][0]
        dy = points[i][1] - points[i + 1][1]      # 屏幕方向（y 向下）
        segs.append((dx, dy))
    total = sum(math.hypot(*s) for s in segs)

    # 「折」的判据：首段与末段都够长，且方向相差很大 —— 真的拐了个直角
    if len(segs) >= 3 and total > 0:
        def norm(s):
            n = math.hypot(*s) or 1.0
            return s[0] / n, s[1] / n
        head = norm(segs[0])
        tail = norm(segs[-1])
        head_len = math.hypot(*segs[0]) / total
        tail_len = math.hypot(*segs[-1]) / total
        dot = max(-1.0, min(1.0, head[0] * tail[0] + head[1] * tail[1]))
        diff = math.degrees(math.acos(dot))
        if diff > 75 and head_len > 0.2 and tail_len > 0.2:
            return 5

    dx = points[-1][0] - points[0][0]
    dy = points[0][1] - points[-1][1]
    if abs(dx) < 1e-6 and abs(dy) < 1e-6:
        return 4
    a = math.degrees(math.atan2(dy, dx))
    if abs(a) <= 30 or abs(a) >= 150:
        return 1
    if 30 < a < 60:
        return 4
    if 60 <= a <= 120:
        return 2
    if 120 < a < 150:
        return 3
    return 1


def load_char_freq(path):
    freq = collections.Counter()
    if not os.path.exists(path):
        return freq
    with open(path, encoding='utf-8') as f:
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
    pkg = sys.argv[1]
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_dir = os.path.join(root, 'app', 'src', 'main', 'assets', 'lexicon')
    os.makedirs(out_dir, exist_ok=True)
    freq = load_char_freq(os.path.join(out_dir, 'zh_pinyin.txt'))

    buckets = collections.defaultdict(list)
    total = 0
    for name in os.listdir(pkg):
        if not name.endswith('.json') or name in ('package.json',):
            continue
        ch = name[:-5]
        if len(ch) != 1 or not ('\u4e00' <= ch <= '\u9fff'):
            continue
        try:
            with open(os.path.join(pkg, name), encoding='utf-8') as f:
                data = json.load(f)
        except Exception:
            continue
        medians = data.get('medians') or []
        if not medians:
            continue
        code = ''.join(str(stroke_type(m)) for m in medians[:MAX_STROKES])
        if not code:
            continue
        buckets[code].append(ch)
        total += 1

    with open(os.path.join(out_dir, 'stroke_index.txt'), 'w', encoding='utf-8') as f:
        for code in sorted(buckets):
            chars = sorted(buckets[code], key=lambda c: -freq.get(c, 0))
            f.write(code + '\t' + ','.join(f'{c}:{freq.get(c, 0) + 1}' for c in chars) + '\n')

    sizes = sorted((len(v) for v in buckets.values()), reverse=True)
    print(f'汉字 {total} 个，笔画码 {len(buckets)} 组，最大组 {sizes[0]}，中位 {sizes[len(sizes)//2]}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
