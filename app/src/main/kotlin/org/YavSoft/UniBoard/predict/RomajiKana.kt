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
 * 罗马字 → 平假名（日文拼音输入）。
 *
 * 按日本常用「ローマ字入力」规则：先长后短匹配（3 → 2 → 1 字符），
 * 促音用叠辅音（kko → っこ），拨音 n 后接非元音转 ん，
 * 未匹配到表里的尾巴原样保留，方便用户继续往下敲。
 */
object RomajiKana {

    private val TABLE: Map<String, String> = buildMap {
        // 清音
        put("a", "あ"); put("i", "い"); put("u", "う"); put("e", "え"); put("o", "お")
        put("ka", "か"); put("ki", "き"); put("ku", "く"); put("ke", "け"); put("ko", "こ")
        put("sa", "さ"); put("shi", "し"); put("si", "し"); put("su", "す"); put("se", "せ"); put("so", "そ")
        put("ta", "た"); put("chi", "ち"); put("ti", "ち"); put("tsu", "つ"); put("tu", "つ")
        put("te", "て"); put("to", "と")
        put("na", "な"); put("ni", "に"); put("nu", "ぬ"); put("ne", "ね"); put("no", "の")
        put("ha", "は"); put("hi", "ひ"); put("fu", "ふ"); put("hu", "ふ"); put("he", "へ"); put("ho", "ほ")
        put("ma", "ま"); put("mi", "み"); put("mu", "む"); put("me", "め"); put("mo", "も")
        put("ya", "や"); put("yu", "ゆ"); put("yo", "よ")
        put("ra", "ら"); put("ri", "り"); put("ru", "る"); put("re", "れ"); put("ro", "ろ")
        put("wa", "わ"); put("wo", "を"); put("nn", "ん"); put("n", "ん")
        // 浊音 / 半浊音
        put("ga", "が"); put("gi", "ぎ"); put("gu", "ぐ"); put("ge", "げ"); put("go", "ご")
        put("za", "ざ"); put("ji", "じ"); put("zi", "じ"); put("zu", "ず"); put("ze", "ぜ"); put("zo", "ぞ")
        put("da", "だ"); put("di", "ぢ"); put("du", "づ"); put("de", "で"); put("do", "ど")
        put("ba", "ば"); put("bi", "び"); put("bu", "ぶ"); put("be", "べ"); put("bo", "ぼ")
        put("pa", "ぱ"); put("pi", "ぴ"); put("pu", "ぷ"); put("pe", "ぺ"); put("po", "ぽ")
        // 拗音
        put("kya", "きゃ"); put("kyu", "きゅ"); put("kyo", "きょ")
        put("sha", "しゃ"); put("shu", "しゅ"); put("sho", "しょ")
        put("sya", "しゃ"); put("syu", "しゅ"); put("syo", "しょ")
        put("cha", "ちゃ"); put("chu", "ちゅ"); put("cho", "ちょ")
        put("tya", "ちゃ"); put("tyu", "ちゅ"); put("tyo", "ちょ")
        put("nya", "にゃ"); put("nyu", "にゅ"); put("nyo", "にょ")
        put("hya", "ひゃ"); put("hyu", "ひゅ"); put("hyo", "ひょ")
        put("mya", "みゃ"); put("myu", "みゅ"); put("myo", "みょ")
        put("rya", "りゃ"); put("ryu", "りゅ"); put("ryo", "りょ")
        put("gya", "ぎゃ"); put("gyu", "ぎゅ"); put("gyo", "ぎょ")
        put("ja", "じゃ"); put("ju", "じゅ"); put("jo", "じょ")
        put("zya", "じゃ"); put("zyu", "じゅ"); put("zyo", "じょ")
        put("bya", "びゃ"); put("byu", "びゅ"); put("byo", "びょ")
        put("pya", "ぴゃ"); put("pyu", "ぴゅ"); put("pyo", "ぴょ")
        // 小假名与长音
        put("xa", "ぁ"); put("xi", "ぃ"); put("xu", "ぅ"); put("xe", "ぇ"); put("xo", "ぉ")
        put("xtu", "っ"); put("ltu", "っ"); put("xya", "ゃ"); put("xyu", "ゅ"); put("xyo", "ょ")
        put("-", "ー")
    }

    /** 把罗马字串转成平假名；未匹配的尾巴原样保留 */
    fun toHiragana(input: String): String {
        if (input.isEmpty()) return ""
        val s = input.lowercase()
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            // 促音：叠辅音（kk / tt / pp / ss …），n 除外
            if (i + 1 < s.length && s[i] == s[i + 1] && s[i] in "bcdfghjklmpqrstvwxyz" && s[i] != 'n') {
                sb.append('っ')
                i += 1
                continue
            }
            var matched = false
            for (len in 3 downTo 1) {
                if (i + len > s.length) continue
                val piece = s.substring(i, i + len)
                val kana = TABLE[piece]
                if (kana != null) {
                    sb.append(kana)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                sb.append(s[i])
                i += 1
            }
        }
        return sb.toString()
    }

    /** 平假名 → 片假名 */
    fun toKatakana(hiragana: String): String {
        val sb = StringBuilder(hiragana.length)
        for (ch in hiragana) {
            val code = ch.code
            sb.append(
                if (code in 0x3041..0x3096) (code + 0x60).toChar() else ch
            )
        }
        return sb.toString()
    }
}
