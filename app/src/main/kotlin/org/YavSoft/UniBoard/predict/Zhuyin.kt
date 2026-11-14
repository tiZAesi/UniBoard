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

package org.YavSoft.UniBoard.predict

import android.icu.text.Transliterator

/**
 * 注音（ㄅㄆㄇㄈ）输入。
 *
 * 键盘敲的是注音符号，内部折算成全拼交给拼音引擎，因此词库、联想、纠错全部复用；
 * 输出可选繁体 —— 繁体由系统 ICU 的 Hans→Hant 转换器完成，不需要额外码表。
 */
object Zhuyin {

    private val INITIALS = mapOf(
        'ㄅ' to "b", 'ㄆ' to "p", 'ㄇ' to "m", 'ㄈ' to "f",
        'ㄉ' to "d", 'ㄊ' to "t", 'ㄋ' to "n", 'ㄌ' to "l",
        'ㄍ' to "g", 'ㄎ' to "k", 'ㄏ' to "h",
        'ㄐ' to "j", 'ㄑ' to "q", 'ㄒ' to "x",
        'ㄓ' to "zh", 'ㄔ' to "ch", 'ㄕ' to "sh", 'ㄖ' to "r",
        'ㄗ' to "z", 'ㄘ' to "c", 'ㄙ' to "s"
    )

    private val MEDIALS = mapOf('ㄧ' to "i", 'ㄨ' to "u", 'ㄩ' to "v")

    private val FINALS = mapOf(
        'ㄚ' to "a", 'ㄛ' to "o", 'ㄜ' to "e", 'ㄝ' to "e",
        'ㄞ' to "ai", 'ㄟ' to "ei", 'ㄠ' to "ao", 'ㄡ' to "ou",
        'ㄢ' to "an", 'ㄣ' to "en", 'ㄤ' to "ang", 'ㄥ' to "eng",
        'ㄦ' to "er"
    )

    /** 声调符号，只用于切分音节，不参与拼音串 */
    private const val TONES = "ˉˊˇˋ˙"

    fun isSymbol(ch: Char): Boolean =
        ch in INITIALS || ch in MEDIALS || ch in FINALS || ch in TONES

    /** 注音串 → 全拼串；非法字符忽略 */
    fun toPinyin(input: String): String {
        val sb = StringBuilder(input.length * 2)
        var initial: String? = null
        var medial: String? = null
        var final: String? = null

        fun flush() {
            if (initial == null && medial == null && final == null) return
            sb.append(syllable(initial, medial, final))
            initial = null; medial = null; final = null
        }

        for (ch in input) {
            when {
                ch in TONES -> flush()
                ch in INITIALS -> {
                    flush()
                    initial = INITIALS[ch]
                }
                ch in MEDIALS -> {
                    if (medial != null) flush()
                    medial = MEDIALS[ch]
                }
                ch in FINALS -> {
                    if (final != null) flush()
                    final = FINALS[ch]
                }
            }
        }
        flush()
        return sb.toString()
    }

    /** 声母 + 介音 + 韵母 拼成合法拼音 */
    private fun syllable(initial: String?, medial: String?, final: String?): String {
        val m = medial
        val f = final
        if (initial == null) {
            // 零声母：i/u/ü 开头要改写
            if (m == "i") {
                return when (f) {
                    null -> "yi"; "a" -> "ya"; "o" -> "yo"; "e" -> "ye"; "ai" -> "yai"
                    "ao" -> "yao"; "ou" -> "you"; "an" -> "yan"; "en" -> "yin"
                    "ang" -> "yang"; "eng" -> "ying"; "ong" -> "yong"; else -> "yi" + f
                }
            }
            if (m == "u") {
                return when (f) {
                    null -> "wu"; "a" -> "wa"; "o" -> "wo"; "ai" -> "wai"; "ei" -> "wei"
                    "an" -> "wan"; "en" -> "wen"; "ang" -> "wang"; "eng" -> "weng"
                    "ong" -> "weng"; else -> "wu" + f
                }
            }
            if (m == "v") {
                return when (f) {
                    null -> "yu"; "e" -> "yue"; "an" -> "yuan"; "en" -> "yun"; else -> "yu" + f
                }
            }
            return f ?: ""
        }

        // 有介音时按汉语拼音的缩写规则拼
        if (m == null) return initial + (f ?: "")
        if (m == "i") {
            val tail = when (f) {
                null -> "i"
                "ou" -> "iu"
                "en" -> "in"
                "eng" -> "ing"
                else -> "i" + f
            }
            return initial + tail
        }
        if (m == "u") {
            val tail = when (f) {
                null -> "u"
                "ei" -> "ui"
                "en" -> "un"
                "eng" -> "ong"
                else -> "u" + f
            }
            return initial + tail
        }
        // ü 在 j/q/x 后写作 u
        val u = if (initial == "j" || initial == "q" || initial == "x") "u" else "v"
        return initial + u + (f ?: "")
    }

    private val hansToHant: Transliterator? by lazy {
        runCatching { Transliterator.getInstance("Hans-Hant") }
            .recoverCatching { Transliterator.getInstance("Simplified-Traditional") }
            .getOrNull()
    }

    /** 简体 → 繁体（系统 ICU） */
    fun toTraditional(text: String): String {
        if (text.isEmpty()) return text
        return runCatching { hansToHant?.transliterate(text) ?: text }.getOrDefault(text)
    }
}
