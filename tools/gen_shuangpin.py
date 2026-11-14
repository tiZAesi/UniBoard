#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""根据 Rime 双拼方案（rime/rime-double-pinyin）的拼写代数，推导各方案的双拼码表。

Rime 每个方案用 speller/algebra 里的 erase / derive / xform / xlit 规则把
「全拼音节」映射成「双拼码」。这里直接把这套规则跑一遍，得到 码 -> 音节 映射，
比手抄键位表可靠：零声母、ü/v、iu/ui、uang/iang 这些容易抄错的边界都由规则本身保证。

产出：predict/ShuangpinTables.kt（Kotlin 常量，运行时零 IO）
"""
import collections
import os
import re

# 各方案 speller/algebra（来源：rime/rime-double-pinyin，GPL-3.0）
ALGEBRAS = {
    'XIAOHE': [  # 小鹤双拼 double_pinyin_flypy
        r'erase/^xx$/',
        r'derive/^([jqxy])u$/$1v/',
        r'derive/^([aoe])([ioun])$/$1$1$2/',
        r'xform/^([aoe])(ng)?$/$1$1$2/',
        r'xform/iu$/Q/', r'xform/(.)ei$/$1W/', r'xform/uan$/R/', r'xform/[uv]e$/T/',
        r'xform/un$/Y/', r'xform/^sh/U/', r'xform/^ch/I/', r'xform/^zh/V/',
        r'xform/uo$/O/', r'xform/ie$/P/', r'xform/i?ong$/S/', r'xform/ing$|uai$/K/',
        r'xform/(.)ai$/$1D/', r'xform/(.)en$/$1F/', r'xform/(.)eng$/$1G/',
        r'xform/[iu]ang$/L/', r'xform/(.)ang$/$1H/', r'xform/ian$/M/',
        r'xform/(.)an$/$1J/', r'xform/(.)ou$/$1Z/', r'xform/[iu]a$/X/',
        r'xform/iao$/N/', r'xform/(.)ao$/$1C/', r'xform/ui$/V/', r'xform/in$/B/',
        r'xlit/QWRTYUIOPSDFGHJKLZXCVBNM/qwrtyuiopsdfghjklzxcvbnm/',
    ],
    'ZIRANMA': [  # 自然码 double_pinyin
        r'erase/^xx$/',
        r'derive/^([jqxy])u$/$1v/',
        r'derive/^([aoe])([ioun])$/$1$1$2/',
        r'xform/^([aoe])(ng)?$/$1$1$2/',
        r'xform/iu$/Q/', r'xform/[iu]a$/W/', r'xform/[uv]an$/R/', r'xform/[uv]e$/T/',
        r'xform/ing$|uai$/Y/', r'xform/^sh/U/', r'xform/^ch/I/', r'xform/^zh/V/',
        r'xform/uo$/O/', r'xform/[uv]n$/P/', r'xform/i?ong$/S/', r'xform/[iu]ang$/D/',
        r'xform/(.)en$/$1F/', r'xform/(.)eng$/$1G/', r'xform/(.)ang$/$1H/',
        r'xform/ian$/M/', r'xform/(.)an$/$1J/', r'xform/iao$/C/', r'xform/(.)ao$/$1K/',
        r'xform/(.)ai$/$1L/', r'xform/(.)ei$/$1Z/', r'xform/ie$/X/', r'xform/ui$/V/',
        r'xform/(.)ou$/$1B/', r'xform/in$/N/',
        r'xlit/QWRTYUIOPSDFGHMJCKLZXVBN/qwrtyuiopsdfghmjcklzxvbn/',
    ],
    'MSPY': [  # 微软双拼 double_pinyin_mspy
        r'erase/^xx$/',
        r'derive/^([jqxy])u$/$1v/',
        r'derive/^([aoe].*)$/o$1/',
        r'xform/^([ae])(.*)$/$1$1$2/',
        r'xform/iu$/Q/', r'xform/[iu]a$/W/', r'xform/er$|[uv]an$/R/', r'xform/[uv]e$/T/',
        r'xform/v$|uai$/Y/', r'xform/^sh/U/', r'xform/^ch/I/', r'xform/^zh/V/',
        r'xform/uo$/O/', r'xform/[uv]n$/P/', r'xform/i?ong$/S/', r'xform/[iu]ang$/D/',
        r'xform/(.)en$/$1F/', r'xform/(.)eng$/$1G/', r'xform/(.)ang$/$1H/',
        r'xform/ian$/M/', r'xform/(.)an$/$1J/', r'xform/iao$/C/', r'xform/(.)ao$/$1K/',
        r'xform/(.)ai$/$1L/', r'xform/(.)ei$/$1Z/', r'xform/ie$/X/', r'xform/ui$/V/',
        r'derive/T$/V/', r'xform/(.)ou$/$1B/', r'xform/in$/N/', r'xform/ing$/;/',
        r'xlit/QWRTYUIOPSDFGHMJCKLZXVBN/qwrtyuiopsdfghmjcklzxvbn/',
    ],
    'ABC': [  # 智能ABC double_pinyin_abc
        r'erase/^xx$/',
        r'derive/^([jqxy])u$/$1v/',
        r'xform/^zh/A/', r'xform/^ch/E/', r'xform/^sh/V/',
        r'xform/^([aoe].*)$/O$1/',
        r'xform/ei$/Q/', r'xform/ian$/W/', r'xform/er$|iu$/R/', r'xform/[iu]ang$/T/',
        r'xform/ing$/Y/', r'xform/uo$/O/', r'xform/uan$/P/', r'xform/i?ong$/S/',
        r'xform/[iu]a$/D/', r'xform/en$/F/', r'xform/eng$/G/', r'xform/ang$/H/',
        r'xform/an$/J/', r'xform/iao$/Z/', r'xform/ao$/K/', r'xform/in$|uai$/C/',
        r'xform/ai$/L/', r'xform/ie$/X/', r'xform/ou$/B/', r'xform/un$/N/',
        r'xform/[uv]e$|ui$/M/',
        r'xlit/QWERTYOPASDFGHJKLZXCVBNM/qwertyopasdfghjklzxcvbnm/',
    ],
    'PYJJ': [  # 拼音加加 double_pinyin_pyjj
        r'erase/^xx$/',
        r'derive/^([jqxy])u$/$1v/',
        r'derive/^([aoe].*)$/o$1/',
        r'xform/^([ae])(.*)$/$1$1$2/',
        r'xform/iu$/N/', r'xform/[iu]a$/B/', r'xform/er$|ing$/Q/', r'xform/[uv]an$/C/',
        r'xform/[uv]e$|uai$/X/', r'xform/^sh/I/', r'xform/^ch/U/', r'xform/^zh/V/',
        r'xform/uo$/O/', r'xform/[uv]n$/Z/', r'xform/i?ong$/Y/', r'xform/[iu]ang$/H/',
        r'xform/(.)en$/$1R/', r'xform/(.)eng$/$1T/', r'xform/(.)ang$/$1G/',
        r'xform/ian$/J/', r'xform/(.)an$/$1F/', r'xform/iao$/K/', r'xform/(.)ao$/$1D/',
        r'xform/(.)ai$/$1S/', r'xform/(.)ei$/$1W/', r'xform/ie$/M/', r'xform/ui$/V/',
        r'xform/(.)ou$/$1P/', r'xform/in$/L/',
        r'xlit/QWRTYUIOPSDFGHMJCKLZXVBN/qwrtyuiopsdfghmjcklzxvbn/',
    ],
}

SYLLABLES = """
a ai an ang ao o ou e ei en eng er
yi ya ye yao you yan yin yang ying yong yu yue
yuan yun wu wa wo wai wei wan wen wang weng ba
bo bai bei bao ban ben bang beng bi bie biao bian
bin bing bu pa po pai pei pao pou pan pen pang
peng pi pie piao pian pin ping pu ma mo me mai
mei mao mou man men mang meng mi mie miao miu mian
min ming mu fa fo fei fou fan fen fang feng fu
da de dai dei dao dou dan den dang deng dong di
die diao diu dian ding du duo dui duan dun ta te
tai tei tao tou tan tang teng tong ti tie tiao tian
ting tu tuo tui tuan tun na ne nai nei nao nou
nan nen nang neng nong ni nie niao niu nian nin niang
ning nu nuo nuan nv nve nun la le lai lei lao
lou lan lang leng long li lia lie liao liu lian lin
liang ling lu luo luan lun lv lve ga ge gai gei
gao gou gan gen gang geng gong gu gua guo guai gui
guan gun guang ka ke kai kei kao kou kan ken kang
keng kong ku kua kuo kuai kui kuan kun kuang ha he
hai hei hao hou han hen hang heng hong hu hua huo
huai hui huan hun huang ji jia jie jiao jiu jian jin
jiang jing jiong ju jue juan jun qi qia qie qiao qiu
qian qin qiang qing qiong qu que quan qun xi xia xie
xiao xiu xian xin xiang xing xiong xu xue xuan xun zha
zhe zhi zhai zhei zhao zhou zhan zhen zhang zheng zhong zhu
zhua zhuo zhuai zhui zhuan zhun zhuang cha che chi chai chao
chou chan chen chang cheng chong chu chua chuo chuai chui chuan
chun chuang sha she shi shai shei shao shou shan shen shang
sheng shu shua shuo shuai shui shuan shun shuang re ri rao
rou ran ren rang reng rong ru rua ruo rui ruan run
za ze zi zai zei zao zou zan zen zang zeng zong
zu zuo zui zuan zun ca ce ci cai cao cou can
cen cang ceng cong cu cuo cui cuan cun sa se si
sai sao sou san sen sang seng song su suo sui suan
sun
""".split()


# 方案在 UI 上的顺序与名字（顺序 = ShuangpinScheme 的 id）
SCHEME_ORDER = [
    ('XIAOHE', '小鹤双拼'),
    ('ZIRANMA', '自然码'),
    ('MSPY', '微软双拼'),
    ('ABC', '智能 ABC'),
    ('PYJJ', '拼音加加'),
]


def parse_rule(rule):
    """`kind/pat/rep/` → (kind, pat, rep)；erase 没有 rep"""
    kind, rest = rule.split('/', 1)
    if rest.endswith('/'):
        rest = rest[:-1]
    if kind == 'erase':
        return kind, rest, ''
    pat, _, rep = rest.partition('/')
    return kind, pat, rep


# 声母按长度降序匹配，zh/ch/sh 先于 z/c/s
INITIALS = ['zh', 'ch', 'sh', 'b', 'p', 'm', 'f', 'd', 't', 'n', 'l', 'g', 'k',
            'h', 'j', 'q', 'x', 'r', 'z', 'c', 's', 'y', 'w']


def expand_repl(m, rep):
    """展开 Rime 的 $1..$9 分组引用"""
    out = []
    i = 0
    while i < len(rep):
        c = rep[i]
        if c == '$' and i + 1 < len(rep) and rep[i + 1].isdigit():
            g = int(rep[i + 1])
            out.append(m.group(g) or '' if g <= (m.re.groups or 0) else '')
            i += 2
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def split_initial(syl):
    for ini in INITIALS:
        if syl.startswith(ini):
            return ini, syl[len(ini):]
    return '', syl


def apply_algebra(syl, rules):
    """按顺序执行 Rime 的拼写代数，返回该音节的所有拼写形式"""
    forms = {syl}
    for rule in rules:
        kind, pat, rep = parse_rule(rule)
        out = set()
        for f in forms:
            m = re.search(pat, f)
            if kind == 'erase':
                if not m:
                    out.add(f)
            elif kind == 'derive':
                out.add(f)
                if m:
                    out.add(f[:m.start()] + expand_repl(m, rep) + f[m.end():])
            elif kind == 'xform':
                if m:
                    out.add(f[:m.start()] + expand_repl(m, rep) + f[m.end():])
                else:
                    out.add(f)
            elif kind == 'xlit':
                table = {c: (rep[i] if i < len(rep) else c) for i, c in enumerate(pat)}
                out.add(''.join(table.get(c, c) for c in f))
            else:
                raise SystemExit('未知规则类型: ' + kind)
        forms = out
    return {f for f in forms if f}


def main():
    result = {}
    for name, rules in ALGEBRAS.items():
        codes = collections.defaultdict(list)   # 2 字符码 -> [音节]
        singles = collections.defaultdict(list)  # 1 字符码 -> [音节]
        bad = []
        for syl in SYLLABLES:
            spellings = apply_algebra(syl, rules)
            if not spellings:
                bad.append((syl, 'erased'))
                continue
            for sp in spellings:
                if len(sp) == 2:
                    codes[sp].append(syl)
                elif len(sp) == 1:
                    singles[sp].append(syl)
                else:
                    bad.append((syl, sp))
        # 同码多音节：保留全部，但按音节字典序稳定排列
        for k in codes:
            codes[k] = sorted(set(codes[k]))
        for k in singles:
            singles[k] = sorted(set(singles[k]))
        # 韵母提示：某个键作为第二键时代表哪些韵母（取最常见的两个）。
        # 只统计「声母+韵母」的音节，零声母别名（小鹤里 an=an 这类）不参与，
        # 否则提示会被别名污染成 an/iao。
        hint_counts = collections.defaultdict(collections.Counter)
        for sp, syls in codes.items():
            for syl in syls:
                ini, fin = split_initial(syl)
                if not ini:
                    continue
                hint_counts[sp[1]][fin] += 1
        hints = {}
        for key, counter in hint_counts.items():
            hints[key] = '/'.join(f for f, _ in counter.most_common(2))
        result[name] = dict(codes=codes, singles=singles, hints=hints, bad=bad)
        print(f'{name}: 双键码 {len(codes)}，单键 {len(singles)}，'
              f'多音节码 {sum(1 for v in codes.values() if len(v) > 1)}，异常 {len(bad)}')
        if bad:
            print('   异常样例:', bad[:10])

    # 生成 Kotlin
    lines = []
    lines.append('/*')
    lines.append(' * UniBoard — Unicode 输入法')
    lines.append(' * Copyright (C) 2026 UniBoard contributors')
    lines.append(' *')
    lines.append(' * 本程序是自由软件：你可以依据 GNU 通用公共许可证第 3 版')
    lines.append(' * 或（你选择的）任何更新版本所规定的条款，重新分发和/或修改它。')
    lines.append(' *')
    lines.append(' * 本程序基于"希望它有用"的目的分发，但不提供任何担保，')
    lines.append(' * 也不承诺适用于特定用途。完整条款见项目根目录的 LICENSE 文件。')
    lines.append(' */')
    lines.append('')
    lines.append('package org.YavSoft.UniBoard.predict')
    lines.append('')
    lines.append('/**')
    lines.append(' * 双拼码表（由 `tools/gen_shuangpin.py` 生成，请勿手工编辑）。')
    lines.append(' *')
    lines.append(' * 码表来自 Rime 各方案的拼写代数（rime/rime-double-pinyin，GPL-3.0）：')
    lines.append(' * 把「全拼音节」按 erase / derive / xform / xlit 规则跑一遍，')
    lines.append(' * 得到「两键码 → 音节」。零声母、ü/v、iu/ui、uang/iang 等边界都由规则保证。')
    lines.append(' */')
    lines.append('internal object ShuangpinTables {')
    lines.append('')
    lines.append('    /** `键位码=音节`，空白分隔；同一码对应多个音节时用 `|` 连接 */')
    for name, data in result.items():
        payload = ' '.join(
            f'{code}={"|".join(syls)}' for code, syls in sorted(data['codes'].items())
        )
        chunks = wrap(payload, 96)
        lines.append(f'    private const val {name} =')
        for i, chunk in enumerate(chunks):
            suffix = '' if i == len(chunks) - 1 else ' +'
            lines.append(f'        "{chunk}"{suffix}')
    lines.append('')
    lines.append('    /** `键位=韵母`，给键盘角标用 */')
    for name, data in result.items():
        payload = ' '.join(f'{k}={v}' for k, v in sorted(data['hints'].items()))
        chunks = wrap(payload, 96)
        lines.append(f'    private const val {name}_HINTS =')
        for i, chunk in enumerate(chunks):
            suffix = '' if i == len(chunks) - 1 else ' +'
            lines.append(f'        "{chunk}"{suffix}')
    lines.append('')
    lines.append('    /** 单键零声母（只有 a / o / e 这类音节才会出现；没有的方案留空串） */')
    for name, _ in SCHEME_ORDER:
        payload = ' '.join(f'{k}={v[0]}' for k, v in sorted(result[name]['singles'].items()))
        lines.append(f'    private const val {name}_SINGLE = "{payload}"')
    lines.append('')
    lines.append('    /** 一个方案的原始码表 */')
    lines.append('    class Raw(val codes: String, val hints: String, val singles: String)')
    lines.append('')
    lines.append('    /** 方案索引 → 原始码表，顺序与 [ShuangpinScheme] 的 id 一致 */')
    lines.append('    val RAW: List<Raw> = listOf(')
    for name, label in SCHEME_ORDER:
        lines.append(f'        Raw({name}, {name}_HINTS, {name}_SINGLE), // {label}')
    lines.append('    )')
    lines.append('}')
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = os.path.join(root, 'app', 'src', 'main', 'kotlin', 'org', 'YavSoft',
                       'UniBoard', 'predict', 'ShuangpinTables.kt')
    with open(out, 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(lines) + '\n')
    print('已写入', out)


def wrap(s, width):
    """按空格切分到不超过 width 的片段（Kotlin 字符串不能跨行）"""
    parts, cur = [], ''
    for token in s.split(' '):
        if cur and len(cur) + 1 + len(token) > width:
            parts.append(cur)
            cur = token
        else:
            cur = (cur + ' ' + token) if cur else token
    if cur:
        parts.append(cur)
    # 关键：Kotlin 里各片段是按 "..." + "..." 直接拼起来的，
    # 片段之间必须自带分隔空格，否则边界处的两个词条会粘成一个
    # （"p=un" + "q=iu" → "p=unq=iu"，丢两条码表）。
    return [p + ' ' for p in parts[:-1]] + parts[-1:]


main()
