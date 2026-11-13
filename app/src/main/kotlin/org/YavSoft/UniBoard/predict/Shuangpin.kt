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

/**
 * 双拼解码（小鹤双拼方案）。
 *
 * 双拼把每个音节压成两键：**声母键 + 韵母键**。
 * 小鹤的声母基本沿用字母，只有三个翘舌音单独占一个键：
 * v→zh、i→ch、u→sh；零声母音节（a、ai、an…）直接按韵母键。
 *
 * 解码结果是一串全拼，直接交给现有的拼音引擎，因此词库、纠错、联想全部复用。
 */
object Shuangpin {

    /** 韵母键位表（小鹤双拼） */
    private val FINALS: Map<Char, String> = mapOf(
        'q' to "iu", 'w' to "ei", 'e' to "e", 'r' to "uan", 't' to "ve",
        'y' to "un", 'u' to "u", 'i' to "i", 'o' to "uo", 'p' to "ie",
        'a' to "a", 's' to "ong", 'd' to "ai", 'f' to "en", 'g' to "eng",
        'h' to "ang", 'j' to "an", 'k' to "ing", 'l' to "iang",
        'z' to "ou", 'x' to "ia", 'c' to "ao", 'v' to "ui",
        'b' to "in", 'n' to "iao", 'm' to "ian"
    )

    /** 声母键位表：小鹤里 zh/ch/sh 各占一键 */
    private val INITIALS: Map<Char, String> = mapOf(
        'v' to "zh", 'i' to "ch", 'u' to "sh",
        'b' to "b", 'p' to "p", 'm' to "m", 'f' to "f", 'd' to "d", 't' to "t",
        'n' to "n", 'l' to "l", 'g' to "g", 'k' to "k", 'h' to "h", 'j' to "j",
        'q' to "q", 'x' to "x", 'r' to "r", 'z' to "z", 'c' to "c", 's' to "s",
        'y' to "y", 'w' to "w"
    )

    /** 零声母音节（按其韵母键单独成音节） */
    private val STANDALONE: Map<Char, String> = mapOf(
        'a' to "a", 'o' to "o", 'e' to "e", 'd' to "ai", 'w' to "ei",
        'c' to "ao", 'z' to "ou", 'j' to "an", 'f' to "en", 'h' to "ang",
        'g' to "eng", 'r' to "er"
    )

    /** 把双拼输入串解码成全拼串 */
    fun toPinyin(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length * 2)
        var i = 0
        while (i < input.length) {
            val first = input[i].lowercaseChar()
            if (i + 1 < input.length) {
                val second = input[i + 1].lowercaseChar()
                val initial = INITIALS[first]
                val fin = FINALS[second]
                if (initial != null && fin != null) {
                    sb.append(initial).append(fin)
                    i += 2
                    continue
                }
            }
            // 单键：零声母音节
            val solo = STANDALONE[first] ?: FINALS[first]
            if (solo != null) {
                sb.append(solo)
                i += 1
            } else {
                sb.append(first)
                i += 1
            }
        }
        return sb.toString()
    }

    /** 键位提示：给双拼键盘显示「该键当韵母时代表什么」 */
    fun finalHint(key: Char): String = FINALS[key.lowercaseChar()] ?: ""
}
