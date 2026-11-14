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
 * 双拼方案。
 *
 * 每个方案都是一张「两键码 → 音节」表，由 `tools/gen_shuangpin.py` 从
 * Rime 的拼写代数生成（见 [ShuangpinTables]）。方案之间只是键位不同，
 * 解码出来的全拼完全一致，因此词库、纠错、联想全部照旧复用。
 */
enum class ShuangpinScheme(val id: Int, val label: String, val short: String) {
    /** 小鹤双拼：最流行的方案，zh/ch/sh 占 v/i/u */
    XIAOHE(0, "小鹤双拼", "小鹤"),

    /** 自然码：老牌方案 */
    ZIRANMA(1, "自然码", "自然码"),

    /** 微软双拼：ing 落在「;」键，键盘要补一个键 */
    MSPY(2, "微软双拼", "微软"),

    /** 智能 ABC：零声母统一加 o 前缀 */
    ABC(3, "智能 ABC", "ABC"),

    /** 拼音加加 */
    PYJJ(4, "拼音加加", "加加");

    /** 该方案的 ing 韵母在「;」键上，键盘需要补一个 ; 键才打得出来 */
    val needsSemicolon: Boolean get() = this == MSPY

    companion object {
        val ALL: List<ShuangpinScheme> = entries

        fun of(id: Int): ShuangpinScheme = entries.getOrElse(id) { XIAOHE }

        fun labels(): List<String> = entries.map { it.label }
    }
}

/**
 * 双拼解码。
 *
 * 双拼把每个音节压成两键：**声母键 + 韵母键**。解码器不逐对贪心切分，而是
 * 在整串输入上做一次动态规划：两键码优先，其次单键零声母，都不认识的键
 * 原样留在拼音串里。这样即使中间有一个键打错，前后已经打对的音节也不会
 * 跟着错位 —— 逐对贪心切分最容易犯的毛病。
 *
 * 解码结果是一串全拼，直接交给现有的拼音引擎。
 */
object Shuangpin {

    /** 一个方案的解码表 */
    class Table(
        /** 两键码 → 音节 */
        val syllables: Map<String, String>,
        /** 单键码 → 零声母音节 */
        val singles: Map<Char, String>,
        /** 键位 → 该键当韵母时代表什么（给键帽角标） */
        val finalHints: Map<Char, String>
    )

    private val cache = HashMap<Int, Table>(4)

    private const val NEG = Int.MIN_VALUE / 4

    /** 命中一个音节得多少分 */
    private const val SCORE_SYLLABLE = 20

    /** 落单一个字符的扣分：够低，但只要别无选择仍然能出字 */
    private const val PENALTY_GAP = 12

    /** 取方案表（首次调用时解析常量串，之后走缓存） */
    fun table(scheme: ShuangpinScheme): Table = synchronized(cache) {
        cache.getOrPut(scheme.id) {
            // 方案 id 越界时退回第一个方案，宁可用错键位也不要崩
            val raw = ShuangpinTables.RAW.getOrElse(scheme.id) { ShuangpinTables.RAW[0] }
            Table(
                syllables = parsePairs(raw.codes).associate { it.first to it.second },
                singles = parsePairs(raw.singles).associate { pair -> pair.first[0] to pair.second },
                finalHints = parsePairs(raw.hints).associate { pair -> pair.first[0] to pair.second }
            )
        }
    }

    private fun parsePairs(src: String): List<Pair<String, String>> {
        if (src.isEmpty()) return emptyList()
        val out = ArrayList<Pair<String, String>>(512)
        for (token in src.split(' ')) {
            if (token.isEmpty()) continue
            val eq = token.indexOf('=')
            if (eq <= 0 || eq == token.length - 1) continue
            // 同码多音节时表里存成 a|b，解码取首选
            out.add(token.substring(0, eq) to token.substring(eq + 1).substringBefore('|'))
        }
        return out
    }

    /** 把双拼输入串解码成全拼串 */
    fun toPinyin(input: String, scheme: ShuangpinScheme = ShuangpinScheme.XIAOHE): String {
        val parts = syllables(input, scheme)
        return parts.joinToString("")
    }

    /**
     * 把双拼输入串切成音节序列。
     *
     * 例如小鹤方案下 `nihcvsgo` → [ni, hao, zhong, guo]。
     * 切不出来的键会原样作为一个片段返回，方便候选栏显示「还差什么」。
     */
    fun syllables(input: String, scheme: ShuangpinScheme = ShuangpinScheme.XIAOHE): List<String> {
        if (input.isEmpty()) return emptyList()
        val t = table(scheme)
        val s = input.lowercase()
        val n = s.length
        val best = IntArray(n + 1) { NEG }
        val from = IntArray(n + 1)
        val piece = arrayOfNulls<String>(n + 1)
        best[0] = 0

        for (i in 0 until n) {
            if (best[i] == NEG) continue
            val base = best[i]
            // 1) 两键码：绝大多数音节走这里
            if (i + 2 <= n) {
                val hit = t.syllables[s.substring(i, i + 2)]
                if (hit != null) {
                    val score = base + SCORE_SYLLABLE
                    if (score > best[i + 2]) {
                        best[i + 2] = score
                        from[i + 2] = i
                        piece[i + 2] = hit
                    }
                }
            }
            // 2) 单键零声母（部分方案把 a / o / e 类音节放在单键上）
            val single = t.singles[s[i]]
            if (single != null) {
                val score = base + SCORE_SYLLABLE - 1
                if (score > best[i + 1]) {
                    best[i + 1] = score
                    from[i + 1] = i
                    piece[i + 1] = single
                }
            }
            // 3) 兜底：这个键没匹配上，原样带下去
            val gap = base - PENALTY_GAP
            if (gap > best[i + 1]) {
                best[i + 1] = gap
                from[i + 1] = i
                piece[i + 1] = s[i].toString()
            }
        }

        val parts = ArrayList<String>(n / 2 + 1)
        var p = n
        while (p > 0) {
            parts.add(piece[p] ?: "")
            p = from[p]
        }
        parts.reverse()
        return parts.filter { it.isNotEmpty() }
    }

    /** 键位提示：该键当韵母时代表什么（给键帽角标用） */
    fun finalHint(key: Char, scheme: ShuangpinScheme = ShuangpinScheme.XIAOHE): String =
        table(scheme).finalHints[key.lowercaseChar()] ?: ""
}
