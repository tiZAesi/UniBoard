/*
 * UniBoard — Unicode 输入法
 * Copyright (C) 2026 UniBoard contributors
 *
 * 本程序是自由软件：你可以依据 GNU 通用公共许可证第 3 版
 * 或（你选择的）任何更新版本所规定的条款，重新分发和/或修改它。
 *
 * 本程序基于"希望它有用"的目的分发，但不提供任何担保，
 * 也不承诺适用于特定用途。完整条款见项目根目录的 LICENSE 文件。
 */

package org.YavSoft.UniBoard.core

/**
 * 键盘种类：**键盘名字与 id 的唯一真相**。
 *
 * 以前这套对应关系散在三个地方（[Prefs] 的常量、键盘选择面板的 if-else、
 * 设置页的选项列表），三份列表长度不一样，于是会出现「面板里切到日文键盘、
 * 进设置页却只看到前三项、一切换就被改回全键盘」这类互相打架的问题。
 * 现在只留这一份，[Prefs.keyboardType] 存的就是这里的 [id]。
 *
 * id 与旧版本保持数值兼容：旧版本里 4 是笔画键盘（已移除），
 * 读到 4 会迁移成全键盘（见 [Prefs.keyboardType]）。
 */
enum class KeyboardKind(
    val id: Int,
    /** 面板与设置页显示的名字 */
    val label: String,
    /** 一行说明，给面板做副标题 */
    val description: String,
    val group: KeyboardGroup,
    /** 是否日文键盘（标点、空格提示、候选链路都按日文走） */
    val japanese: Boolean = false,
    /** 是否依赖异步加载的词库（未加载完时面板要给出提示，而不是静默退回全键盘） */
    val needsLexicon: Boolean = false
) {
    /** 全键盘：拼音 / 英文共用，也能挂数字行 */
    FULL(0, "全键盘", "26 键全拼，可开简拼与数字行", KeyboardGroup.CHINESE),

    /** 九键（T9） */
    NINE(1, "九键（T9）", "九宫格拼音，单手更好按", KeyboardGroup.CHINESE),

    /** 明快键盘：两键取字 */
    MINGKWAI(2, "明快键盘", "72 个字根，上形＋下形取字", KeyboardGroup.CHINESE, needsLexicon = true),

    /** 双拼 */
    SHUANGPIN(3, "双拼", "两键一音，方案可选", KeyboardGroup.CHINESE),

    /** 英文九键 */
    T9_EN(5, "英文九键", "九宫格英文联想", KeyboardGroup.OTHER),

    /** 日文（罗马字） */
    KANA(6, "日文（罗马字）", "罗马字 → 假名 → 汉字", KeyboardGroup.OTHER,
        japanese = true, needsLexicon = true),

    /** 注音 */
    ZHUYIN(7, "注音（ㄅㄆㄇㄈ）", "注音符号，可输出繁体", KeyboardGroup.OTHER),

    /** 日文（五十音）：直接点假名，同样能转汉字 */
    KANA50(8, "日文（五十音）", "直接点假名，再按空格转汉字", KeyboardGroup.OTHER,
        japanese = true, needsLexicon = true);

    /** 该键盘是否走「组合串 + 候选栏」这条链路（日文与中文一样需要组合） */
    val composing: Boolean get() = this != T9_EN

    companion object {
        /** 旧版本里笔画键盘的 id，只用于读旧设置时迁移 */
        const val LEGACY_STROKE_ID = 4

        /** 最大 id，用于 [Prefs] 的范围校验 */
        const val MAX_ID = 8

        val ALL: List<KeyboardKind> = entries

        fun of(id: Int): KeyboardKind = when (id) {
            LEGACY_STROKE_ID -> FULL
            else -> entries.firstOrNull { it.id == id } ?: FULL
        }

        /** 按常量名取（给配置、调试用） */
        fun ofName(name: String): KeyboardKind =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FULL

        val chinese: List<KeyboardKind> = entries.filter { it.group == KeyboardGroup.CHINESE }

        val others: List<KeyboardKind> = entries.filter { it.group == KeyboardGroup.OTHER }
    }
}

/** 键盘分组：选择面板按这个分组排版 */
enum class KeyboardGroup(val label: String) {
    CHINESE("中文输入"),
    OTHER("其它语言")
}
