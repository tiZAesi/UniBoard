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
 * 罗马字 → 平假名（日文「ローマ字入力」）。
 *
 * 规则按日本输入法的通行做法：
 * - 先长后短匹配（4 → 3 → 2 → 1 字符），`kya` 优先于 `k` + `ya`；
 * - 促音用叠辅音（`kko` → っこ，`tch` → っち）；
 * - `n` 在辅音前是拨音（`kan` → かん），`nn` 永远是拨音，
 *   但 `ny` / `na` 这类后面接元音或 y 的情况仍然是一个完整音节；
 * - 结尾还没打完的半截罗马字（`ky`）单独作为 [Result.pending] 返回，
 *   不参与假名转换，于是「打一半」时不会把 `k` 当成一个假名吐出来。
 *
 * 日文汉字转换由 [KanaKanji] 负责，这里只管罗马字 → 假名这一段。
 */
object RomajiKana {

    /**
     * 转换结果。
     *
     * @param kana 已经确定的假名
     * @param pending 末尾还没打完的罗马字（可能为空）
     */
    data class Result(val kana: String, val pending: String) {
        val isPending: Boolean get() = pending.isNotEmpty()
        /** 假名部分 + 未打完的罗马字，用于候选栏预览 */
        val display: String get() = kana + pending
    }

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
        put("wi", "うぃ"); put("we", "うぇ")
        // 浊音 / 半浊音
        put("ga", "が"); put("gi", "ぎ"); put("gu", "ぐ"); put("ge", "げ"); put("go", "ご")
        put("za", "ざ"); put("ji", "じ"); put("zi", "じ"); put("zu", "ず"); put("ze", "ぜ"); put("zo", "ぞ")
        put("da", "だ"); put("di", "ぢ"); put("du", "づ"); put("de", "で"); put("do", "ど")
        put("ba", "ば"); put("bi", "び"); put("bu", "ぶ"); put("be", "べ"); put("bo", "ぼ")
        put("pa", "ぱ"); put("pi", "ぴ"); put("pu", "ぷ"); put("pe", "ぺ"); put("po", "ぽ")
        put("vu", "ゔ")
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
        put("jya", "じゃ"); put("jyu", "じゅ"); put("jyo", "じょ")
        put("zya", "じゃ"); put("zyu", "じゅ"); put("zyo", "じょ")
        put("bya", "びゃ"); put("byu", "びゅ"); put("byo", "びょ")
        put("pya", "ぴゃ"); put("pyu", "ぴゅ"); put("pyo", "ぴょ")
        put("dya", "ぢゃ"); put("dyu", "ぢゅ"); put("dyo", "ぢょ")
        // 外来语常见组合
        put("fa", "ふぁ"); put("fi", "ふぃ"); put("fe", "ふぇ"); put("fo", "ふぉ"); put("fyu", "ふゅ")
        put("thi", "てぃ"); put("dhi", "でぃ")
        put("she", "しぇ"); put("je", "じぇ"); put("che", "ちぇ"); put("ye", "いぇ")
        put("kwa", "くぁ"); put("tsa", "つぁ"); put("tse", "つぇ"); put("tso", "つぉ")
        // 小假名与促音
        put("xa", "ぁ"); put("xi", "ぃ"); put("xu", "ぅ"); put("xe", "ぇ"); put("xo", "ぉ")
        put("la", "ぁ"); put("li", "ぃ"); put("lu", "ぅ"); put("le", "ぇ"); put("lo", "ぉ")
        put("xtu", "っ"); put("ltu", "っ"); put("xtsu", "っ"); put("ltsu", "っ")
        put("xya", "ゃ"); put("xyu", "ゅ"); put("xyo", "ょ")
        put("lya", "ゃ"); put("lyu", "ゅ"); put("lyo", "ょ")
        put("xwa", "ゎ"); put("lwa", "ゎ")
        // 长音符与标点
        put("-", "ー")
    }

    /** 所有表项的「真前缀」集合：用来判断末尾是不是还没打完 */
    private val PREFIXES: Set<String> = buildSet {
        for (key in TABLE.keys) {
            for (len in 1 until key.length) add(key.substring(0, len))
        }
    }

    private const val MAX_KEY = 4

    private fun isConsonant(ch: Char): Boolean = ch in "bcdfghjklmpqrstvwxyz"

    private fun isVowel(ch: Char): Boolean = ch in "aiueo"

    /** 罗马字 → 假名（含未打完的尾巴） */
    fun convert(input: String): Result {
        if (input.isEmpty()) return Result("", "")
        val s = input.lowercase()
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val ch = s[i]

            // 促音：叠辅音（n 除外）；tch / cch 也算促音
            if (i + 1 < s.length && ch == s[i + 1] && isConsonant(ch) && ch != 'n') {
                sb.append('っ')
                i += 1
                continue
            }
            if (ch == 't' && i + 2 < s.length && s[i + 1] == 'c' && s[i + 2] == 'h') {
                sb.append('っ')
                i += 1
                continue
            }

            // 拨音：n 后接辅音（y 除外）或 n 时是 ん
            if (ch == 'n' && i + 1 < s.length) {
                val next = s[i + 1]
                if (next == '\'') {
                    sb.append('ん')
                    i += 2
                    continue
                }
                if (next == 'n' && (i + 2 >= s.length || !isVowel(s[i + 2]))) {
                    sb.append('ん')
                    i += 2
                    continue
                }
                if (isConsonant(next) && next != 'y') {
                    sb.append('ん')
                    i += 1
                    continue
                }
            }

            // 先长后短匹配
            var matched = false
            var len = minOf(MAX_KEY, s.length - i)
            while (len >= 1) {
                val piece = s.substring(i, i + len)
                val kana = TABLE[piece]
                if (kana != null) {
                    sb.append(kana)
                    i += len
                    matched = true
                    break
                }
                len -= 1
            }
            if (!matched) {
                // 末尾若是某个表项的前半截，说明还没打完：留给 pending
                val tail = s.substring(i)
                if (tail in PREFIXES) {
                    return Result(sb.toString(), tail)
                }
                sb.append(s[i])
                i += 1
            }
        }
        return Result(sb.toString(), "")
    }

    /** 把罗马字串转成平假名；末尾未打完的罗马字原样保留在结果里 */
    fun toHiragana(input: String): String {
        val r = convert(input)
        return r.kana + r.pending
    }

    /** 只要假名部分，丢掉未打完的罗马字尾巴 */
    fun kanaOnly(input: String): String = convert(input).kana

    /** 平假名 → 片假名 */
    fun toKatakana(hiragana: String): String {
        val sb = StringBuilder(hiragana.length)
        for (ch in hiragana) {
            val code = ch.code
            sb.append(if (code in 0x3041..0x3096) (code + 0x60).toChar() else ch)
        }
        return sb.toString()
    }

    /** 片假名 → 平假名（词典读音归一用） */
    fun toHiraganaFromKatakana(katakana: String): String {
        val sb = StringBuilder(katakana.length)
        for (ch in katakana) {
            val code = ch.code
            sb.append(if (code in 0x30A1..0x30F6) (code - 0x60).toChar() else ch)
        }
        return sb.toString()
    }

    /** 是否是假名（含长音符），用于判断一个读音能不能进假名链路 */
    fun isKana(ch: Char): Boolean {
        val c = ch.code
        return c in 0x3041..0x3096 || c in 0x30A1..0x30FA || c == 0x30FC || c == 0x3099 || c == 0x309A
    }

    /** 清音 → 浊音 */
    private val VOICE: Map<Char, Char> = mapOf(
        'か' to 'が', 'き' to 'ぎ', 'く' to 'ぐ', 'け' to 'げ', 'こ' to 'ご',
        'さ' to 'ざ', 'し' to 'じ', 'す' to 'ず', 'せ' to 'ぜ', 'そ' to 'ぞ',
        'た' to 'だ', 'ち' to 'ぢ', 'つ' to 'づ', 'て' to 'で', 'と' to 'ど',
        'は' to 'ば', 'ひ' to 'び', 'ふ' to 'ぶ', 'へ' to 'べ', 'ほ' to 'ぼ',
        'う' to 'ゔ', 'ゝ' to 'ゞ'
    )

    /** は行 → 半浊音 */
    private val SEMI: Map<Char, Char> = mapOf(
        'は' to 'ぱ', 'ひ' to 'ぴ', 'ふ' to 'ぷ', 'へ' to 'ぺ', 'ほ' to 'ぽ'
    )

    /** 浊音 → 清音（点错了再点一下就能改回来） */
    private val DEGRADE: Map<Char, Char> = VOICE.entries.associate { (k, v) -> v to k }

    /**
     * 给最后一个假名加浊点 / 半浊点。
     *
     * @return 合并后的字符串；最后一个字符不能加浊点时返回 null
     */
    fun voice(kana: String, handakuten: Boolean = false): String? {
        if (kana.isEmpty()) return null
        val last = kana.last()
        val katakana = last.code in 0x30A1..0x30F6
        val hira = if (katakana) (last.code - 0x60).toChar() else last
        val mapped = when {
            handakuten -> SEMI[hira]
            else -> VOICE[hira] ?: DEGRADE[hira]
        } ?: return null
        val out = if (katakana) (mapped.code + 0x60).toChar() else mapped
        return kana.dropLast(1) + out
    }
}
