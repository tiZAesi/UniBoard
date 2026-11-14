# UniBoard

[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0--or--later-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3ddc84.svg)](#构建)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-7f52ff.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/version-LabicBuild--1.0.4.1-orange.svg)](CHANGELOG.md)

一个以 **Unicode 为核心**、同时把中日文输入做全的 Android 输入法。

默认键盘不是 QWERTY，而是**直输盘**：数字、常用符号加一个 `U+` 键，按下就把字符送
上屏。需要打字时再切到全键盘、九键、双拼、注音或日文键盘 —— 每种键盘都是一等公民，
不是附属插件。

自由软件，以 GPL-3.0-or-later 发布。

---

## 目录

- [为什么做这个](#为什么做这个)
- [功能总览](#功能总览)
- [键盘一览](#键盘一览)
- [输入示例](#输入示例)
- [构建](#构建)
- [词库与生成脚本](#词库与生成脚本)
- [项目结构](#项目结构)
- [隐私](#隐私)
- [已知限制](#已知限制)
- [常见问题](#常见问题)
- [参与贡献](#参与贡献)
- [许可证](#许可证)
- [数据来源](#数据来源)

---

## 为什么做这个

手机输入法大多把「输入法」等同于「拼音 / 英文」。但 Unicode 已有三万多个已分配字符：
箭头、数学符号、注音、彝文、CJK 扩展区、各种图形符号。想输入这些字符，通常得开网页查码点。

UniBoard 把这件事变简单：默认键盘就是一张字符表，`U+` 键可以直接按码点取任意字符，
输入 `4E0` 会列出 `4E00–4E0F` 区间的字符，字母误输（`O`→`0`、`I`→`1`）也会自动纠正。

同时它没有把中文、日文当成「顺便支持一下」：中文有全拼 / 简拼 / 九键 / 双拼（5 种方案）/
注音 / 明快两键取字，日文有罗马字与五十音两种键盘，并且**带假名→汉字转换**。全部离线，
不带任何在线词库。

---

## 功能总览

### 输入

| 能力 | 说明 |
|---|---|
| Unicode 直输 | 默认键盘，数字 / 符号 / `U+` 码点直接上屏 |
| 中文全拼 | 86,653 词条，词频排序 |
| 简拼 | 39,282 条索引，`zg` → 中国 |
| 模糊音 | `zh/z`、`n/l`、`an/ang` 等，仅在没有精确命中时启用 |
| 双拼 | 小鹤 / 自然码 / 微软双拼 / 智能 ABC / 拼音加加，键位角标随方案变化 |
| 九键（T9） | 中文九宫格拼音，数字键只作编码 |
| 注音 | 37 个注音符号 + 声调，可输出繁体 |
| 日文 | 罗马字 → 假名 → 汉字；另有五十音直点键盘 |
| 明快键盘 | 72 个字根，上形＋下形两键取字，长按看其它写法 |
| 英文 | 前缀联想 + 编辑距离 / 相邻键纠错，可挂数字行 |
| 中英混输 | 拼音候选里内联英文词，免去来回切模式 |
| 用户学习 | 词频、上下文二元组、自动收录，全部本地存储 |
| 外接键盘 | 蓝牙 / USB 键盘按桌面输入法习惯工作，可设「固定英文直出」 |

### 日文（本轮新增）

- 罗马字输入按「ローマ字入力」规则处理：促音（`kko`→っこ）、拨音（`kan`→かん）、
  拗音、外来语组合（`fa`→ふぁ）、小假名（`xa`→ぁ）、长音符（`-`→ー）
- 打完的假名进组合串，空格取第一个候选（＝変換），候选栏同时给出其它写法、片假名、平假名
- 转换用动态规划在整串假名上找最低代价切分，不是贪心最长匹配：
  `きょうはいいてんきだ` 这种长句也能整句转换，词表里没有的助词原样保留
- 五十音键盘「先点假名、再点 ゛ / ゜」合成浊音半浊音（か＋゛→が），点错了再点一次退回清音
- 罗马字键盘与五十音键盘用一个键互相切换

### Unicode

- 码点解析支持 `4E00`、`U+4E00`、`0x4E00`、`&#x4E00;`、`&#19968;`
- 码点容错与前端联想（输入 `4E0` 列出邻近码点）
- 字符名检索（搜 `SNOWMAN`、`HEART`）、33 个区块浏览、最近使用
- 逐字符字形检测，回退链：基准字体 → 导入字体 → 内置 GNU Unifont → 系统字体

### 面板与工具

- Emoji 面板：7 个分类约 700 个表情，支持「笑 / smile / heart」这类语义候选
- 剪贴板面板：读取当前剪贴板、收藏常用片段、按天自动清理
- Unicode 面板：码点、名称、区块三种取字方式
- 文本编辑面板：光标移动、全选、复制粘贴、左右微调
- 键盘选择面板：一个面板列全部键盘，带一行说明与当前项打勾

### 定制

- 主题：跟随系统 / 浅色 / 深色，8 色主题色板 + 动态取色；深色可切纯黑底（OLED）
- 深色模式有**对比度验收**：底色方向或正文对比度不达标时自动回落到内置调色板，
  不会再出现「浅底白字」
- 键盘高度、行高、圆角、字号、不透明度、候选数量、长按响应时间
- 长按备选字符（重音、引号形态）、上滑符号、滑行输入、空格滑动移光标、双击空格句号
- 输入偏好脚本：字面替换、正则替换、`{date}` `{time}` `{clip}` 变量
- 候选权重可调：整词命中、本机词频、长词、日常词、简拼、中英混输条数
- 输入统计：累计 / 今日字数、按键次数、上屏次数，只看本机
- 学习记录日志：应用内直接看最近上屏、词频排行、联想关系，可导出/清空

### AI（可选，默认关闭）

- 默认对接 DeepSeek，也可换成任何 OpenAI 兼容接口（含本地模型）
- 点候选栏星形键 = 润色当前输入；长按 = 联想后续短语
- 内置提示词要求模型「只输出结果、不要解释」
- 关闭后应用完全不联网；隐私模式可强制只允许本地 / 局域网模型

---

## 键盘一览

| 键盘 | 适合场景 | 说明 |
|---|---|---|
| 全键盘 | 中文拼音 / 英文 | QWERTY，可开数字行、简拼、中英混输 |
| 九键（T9） | 单手中文 | 九宫格拼音，数字键仅作编码 |
| 双拼 | 想少按键的中文用户 | 5 种方案（**长按切换**），键帽角标显示该键当韵母代表什么 |
| 明快键盘 | 想按字形找字 | 72 字根两页，上形＋下形取字 |
| 注音 | 台湾用户 | ㄅㄆㄇㄈ，可切繁体输出 |
| 英文九键 | 单手英文 | 九宫格英文联想 |
| 日文（罗马字） | 会键盘打日文 | 罗马字 → 假名 → 汉字 |
| 日文（五十音） | 不想记罗马字 | 直接点假名，同样是「假名 → 汉字」 |

> 笔画输入已在 1.0.4.0 移除：它的五笔编码是离线从字形中线自动推断的，遇上横折被平滑成
> 弧形的情况必然判错，容错只能靠「允许错一位」硬撑，实际命中率不高。选过它的用户升级后
> 会自动回到全键盘，不会留下一个选不中又切不走的幽灵项。

---

## 输入示例

**Unicode 码点**

```
按 U+ → 4E0 → 候选栏列出 4E00 一 / 4E01 丁 / 4E02 丂 …
按 U+ → 1F600 → 😀
```

**中文拼音 / 简拼 / 双拼**

```
nihao      → 你好
zg         → 中国（简拼）
nihc       → 你好（小鹤双拼：ni + hc(hao)）
vygo       → zhong + guo → 中国（小鹤：vy=zhong，go=guo）
状态位显示音节切分：ni hao
```

**日文**

```
kanji      → かんじ → 漢字
gakkou     → がっこう → 学校
nihongo    → にほんご → 日本語
ky         → 组合区显示 ky（半截罗马字不算假名）
```

**注音**

```
ㄋㄧˇ ㄏㄠˇ → 你好
```

---

## 构建

要求 **JDK 17**、**Android SDK 34**。

```bash
git clone https://github.com/tiZAesi/UniBoard.git
cd UniBoard
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug
```

产物在 `app/build/outputs/apk/debug/`。也可以直接用 Android Studio 打开工程。

| 项 | 值 |
|---|---|
| 最小支持 | Android 8.0（API 26） |
| 编译 / 目标 SDK | 34 |
| Kotlin | 1.9.24 |
| AGP | 8.5.2 |
| Gradle | 8.7 |

仓库里带了一份 GitHub Actions 工作流（自检 + 单测 + 打 APK），放在
`tools/ci/android-workflow.yml`：因为推送令牌没有 `workflow` scope，暂时不能直接放进
`.github/workflows/`。想启用就把它复制过去：

```bash
mkdir -p .github/workflows && cp tools/ci/android-workflow.yml .github/workflows/android.yml
git add .github/workflows/android.yml && git commit -m "启用 CI" && git push
```

跑一遍单元测试与静态自检：

```bash
./gradlew :app:testDebugUnitTest
python3 tools/check_code.py
```

`check_code.py` 检查三类提交时最容易漏掉的问题：`R.*` 引用了不存在的资源、
花括号 / 圆括号不配平、文件缺少 GPL 版权头；另外挡了一条 Android 14 的坑 ——
`Settings.Secure.getString` 在 `targetSdk 34` 上会抛 `SecurityException`，
裸读一次就是「启动即闪退」，必须包在 `runCatching` 里。

---

## 词库与生成脚本

词库都不手写，而是由脚本从公开数据生成；生成结果随仓库分发，改数据源后重跑即可。

| 脚本 | 产物 | 数据源 |
|---|---|---|
| `tools/gen_lexicon.py` | `zh_pinyin.txt`、`zh_initials.txt`、`en_words.txt` | jieba 词典、google-10000-english（+ pypinyin） |
| `tools/gen_mingkwai.py` | `mingkwai_keys.txt`、`mingkwai_index.txt` | 明快打字机字根表 |
| `tools/gen_shuangpin.py` | `predict/ShuangpinTables.kt` | Rime 各方案的拼写代数（rime-double-pinyin） |
| `tools/gen_ja_dict.py` | `ja_kanji.lex` | JMdict（常用词 + 全量补覆盖）、KANJIDIC2 |

```bash
python3 tools/gen_shuangpin.py     # 双拼码表（纯计算，无需下载）
python3 tools/gen_ja_dict.py       # 日文假名→汉字词表（--verify 顺带回读自检）
pip install pypinyin
python3 tools/gen_lexicon.py       # 中文 / 英文词库
```

几个数字（1.0.4.1）：

- 中文：86,653 个词条，39,282 条简拼索引
- 英文：11,816 个词条
- 明快：72 个字根键，1,984 个字形桶
- 日文：102,632 行读音，163,901 组「读音 → 表记」，gzip 后 1.3 MB

日文词表的成本（cost）尺度和 Kotlin 侧两个常数（`passthroughCost`、`segmentCost`）
是配套的：最常用词条 100，单字表记因为容易误抢被加了 1500 惩罚、落在 1600 上下。
改生成规则时请一并复核 `predict/KanaKanji.kt` 里的注释，否则会出现「助词被莫名换成汉字」
或者「やま 永远转不出 山」这类问题。

---

## 项目结构

```
app/src/main/kotlin/org/YavSoft/UniBoard/
├── ime/       输入法服务：按键分发、键盘解析、模式切换
├── keyboard/  自绘键盘、候选栏、布局定义
├── predict/   词库索引、联想、纠错、用户学习、双拼、假名转换
├── unicode/   码点解析、Unicode 面板、最近记录
├── emoji/     表情数据与面板
├── clip/      剪贴板仓库与面板
├── script/    输入偏好脚本引擎
├── ai/        DeepSeek / OpenAI 兼容客户端
├── font/      字形覆盖检测与字体回退
├── core/      偏好设置、键盘清单、输入统计、学习日志
└── ui/        设置页、关于页、许可证页、选择面板
tools/         词库生成脚本与静态自检
```

两条关键约定：

1. **键盘清单只有一份**：`core/KeyboardKind.kt`。名字、id、顺序、分组都在那里，
   `Prefs.keyboardType` 存的就是它的 id。以前面板、设置页、输入法服务各抄一份清单，
   三份长度不一样，是「切换键盘没反应」最常见的成因，现在不允许再出现第二份。
2. **屏幕上显示哪张键盘只由 `resolveLayout()` 决定**，输入法服务里不再维护第二份状态。
   切换键盘时统一清掉页面状态与大写状态，避免「选了九键还停在符号页」。

---

## 隐私

- 不收集设备标识，无统计、无广告、无第三方 SDK
- `INTERNET` 权限只为用户主动触发的 AI 请求存在
- 词频、剪贴板收藏、脚本、输入统计、最近字符全部写在应用私有目录
- 唯一读取的系统敏感信息是剪贴板，且只在你主动打开剪贴板面板时读取
- 密码框（系统标记为 password 的输入框）不联想、不学习、不落盘
- 日文词表、中文词库、Unicode 数据全部离线打包，输入过程零联网

---

## 已知限制

- 系统自 Android 10 起不再开放剪贴板历史，因此剪贴板面板只能读「当前内容 + 用户手动收藏」
- 输入法无法控制目标应用的字体，字形回退只作用于本应用自己的候选栏与面板
- 手写、语音输入尚未实现
- 日文词表是「常用词 + 常用汉字单字」的精简表，不是完整 JMdict：
  生僻专有名词、非常用外来语可能转不出来，此时会退化成假名候选，不会吞字
- 韩文、泰文等其它语言的键盘尚未支持（Unicode 面板可以逐字取）

---

## 常见问题

**Q：切换键盘点了没反应？**

1.0.4.0 修掉了三个真实成因：① `shiftOn`（英文句末自动大写、按过 Shift）会盖住键盘种类
判断，导致切到九键 / 明快 / 双拼仍显示全键盘；② 停在符号页或明快第二页时切键盘，页面状态
优先级更高，看到的还是原来那一页；③ 明快 / 日文的词库异步加载，加载完成前只能先退回全键盘，
而加载完成后没有人重排。现在切换会清空页面与大写状态，词库加载完会自动换回真正的键盘。

**Q：深色模式下键盘变成浅底白字，看不清？**

1.0.4.1 修掉了这条链路上的四个点：深浅色判定在 `uiMode` 说不清时会再问
`UiModeManager`；配色解析完要做一次对比度验收（底色方向错或对比度 < 4.5:1 就回落到内置
调色板）；系统切主题时输入法会重新解析并应用配色（输入法窗口不会自己重建）；
应用内的说明文字也改成了主题色。如果仍然觉得刺眼，可以在「外观与主题 → 深色外观」里
打开「深色用纯黑底」。

**Q：日文打不出汉字？**

确认选的是「日文（罗马字）」或「日文（五十音）」键盘（候选栏右上角显示「あ」），
并且设置里的「日文汉字转换」是打开的。打完假名后**按空格**取第一个候选即为変換结果，
候选栏里也会列出其它写法、片假名与平假名。

**Q：为什么默认是 Unicode 盘而不是字母键盘？**

这是本项目的立意：先解决「想输入任意字符」，再解决「打字」。可以在
「设置 → 键盘布局与手感 → 默认输入模式」里改成中文或英文启动。

**Q：词库能自己加词吗？**

可以。设置页有「导入词库」（每行一词，可带权重），输入过程中自动收录的词也写进同一份
用户词典；这些数据都在应用私有目录，卸载即删。

---

## 参与贡献

见 [CONTRIBUTING.md](CONTRIBUTING.md)。提交前请至少跑一次：

```bash
python3 tools/check_code.py
./gradlew :app:testDebugUnitTest
```

新增键盘或改键位时，请同步更新 `core/KeyboardKind.kt`（清单）、
`ui/KeyboardPickerPanelView.kt` 会跟着枚举自动更新，以及 README 的键盘表。

---

## 许可证

本项目以 [GNU 通用公共许可证第 3 版或更高版本](LICENSE) 发布。

```
Copyright (C) 2026 UniBoard contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

再分发二进制时须同时提供对应源代码，或给出书面获取途径。

---

## 数据来源

| 项目 | 用途 | 许可 |
|---|---|---|
| [jieba](https://github.com/fxsjy/jieba) | 中文词典与词频 | MIT |
| [google-10000-english](https://github.com/first20hours/google-10000-english) | 英文词表 | 公共领域 |
| [pypinyin](https://github.com/mozillazg/python-pinyin) | 词库拼音生成 | MIT |
| [GNU Unifont](https://unifoundry.com/unifont/) | 生僻字兜底字体 | SIL OFL 1.1 |
| [rime-double-pinyin](https://github.com/rime/rime-double-pinyin) | 双拼码表的拼写代数 | GPL-3.0 |
| [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html) | 日文读音 → 表记 | CC BY-SA 4.0 |
| [KANJIDIC2](https://www.edrdg.org/wiki/index.php/KANJIDIC_Project) | 日文单字读音与字频 | CC BY-SA 4.0 |

日文词表（`assets/lexicon/ja_kanji.lex`）是上述 JMdict / KANJIDIC2 的**衍生数据**，
同样以 CC BY-SA 4.0 提供，可由 `tools/gen_ja_dict.py` 完整重建。

应用图标由本项目自行设计，不含任何第三方商标元素。

---

## 相关链接

- 仓库：<https://github.com/tiZAesi/UniBoard>
- 更新记录：[CHANGELOG.md](CHANGELOG.md)
- 问题反馈：<https://github.com/tiZAesi/UniBoard/issues>
