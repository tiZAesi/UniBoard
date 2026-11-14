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

import java.io.BufferedReader

/**
 * 假名 → 汉字 转换（日文输入法的「変換」步骤）。
 *
 * 词表来自 `assets/lexicon/ja_kanji.lex`（gzip 压缩的文本），由 `tools/gen_ja_dict.py` 从
 * JMdict（精简常用词）与 KANJIDIC2 生成，格式一行一条：
 *
 * ```
 * よみ<TAB>表記:コスト,表記:コスト,…
 * ```
 *
 * 转换用动态规划在整串假名上找最低代价切分，而不是贪心取最长匹配：
 * 「きょうはいいってんきだ」这种长句，贪心会在「きょう」和后面的助词上翻车，
 * DP 会同时考虑「文節 + 助词原样保留」的所有组合，代价最低的那条就是候选 1。
 *
 * 词表里没有的假名（助词、外来语、拟声词）原样保留，所以即使词表很小，
 * 输出也永远是合法日文，不会出现「吃掉假名」的情况。
 */
class KanaKanji {

    /** 一条表记候选 */
    data class Entry(val surface: String, val cost: Int)

    /** 读音 → 候选（按 cost 升序） */
    private val dict = HashMap<String, List<Entry>>(1 shl 15)

    private var maxReading = 1

    @Volatile
    var isLoaded: Boolean = false
        private set

    /** 词条总数，给设置页显示 */
    @Volatile
    var entryCount: Int = 0
        private set

    /**
     * 每多切一个文節增加的代价：稍微倾向「长一点的词条」。
     *
     * 这两个常数要和词表的 cost 尺度配套（词表四档：常用词条 100、
     * 有常用写法 200、汉字+送假名混排 +300、非常用词条 2400；
     * 单假名读音的单字表记另有 +1200 罚，避免把助词换成汉字）：
     * - 单假名读音（は / に / を）原样保留 1200+200，低于单字表记，
     *   所以助词不会被莫名换成汉字；
     * - 「やま」这种两假名对应一个汉字的读音，原样保留要 2600，
     *   高于 山(100)+200，于是正常转成汉字；
     * - 「はい」这种读音，非常用词条 廃位(2400) 会被 2600 的保留代价压住，
     *   不会把「今日はいい天気だ」切成「今日廃位天気だ」。
     * 改动词表生成脚本的 cost 规则时，请一并复核这两个数。
     */
    private val segmentCost = 200.0

    /** 词表里查不到、按原样保留一个假名的代价 */
    private val passthroughCost = 1200.0

    /** 兜底：整串都没有转换时，至少给出片假名/平假名 */
    fun load(reader: BufferedReader) {
        val local = HashMap<String, List<Entry>>(1 shl 15)
        var count = 0
        reader.forEachLine { line ->
            if (line.isEmpty()) return@forEachLine
            val tab = line.indexOf('\t')
            if (tab <= 0 || tab == line.length - 1) return@forEachLine
            val reading = line.substring(0, tab)
            if (reading.isEmpty()) return@forEachLine
            val entries = ArrayList<Entry>(8)
            for (item in line.substring(tab + 1).split(',')) {
                if (item.isEmpty()) continue
                val colon = item.lastIndexOf(':')
                if (colon <= 0) continue
                val surface = item.substring(0, colon)
                val cost = item.substring(colon + 1).toIntOrNull() ?: 5000
                if (surface.isEmpty()) continue
                entries.add(Entry(surface, cost))
            }
            if (entries.isNotEmpty()) {
                entries.sortWith(compareBy({ it.cost }, { it.surface }))
                local[reading] = entries
                count += entries.size
                if (reading.length > maxReading) maxReading = reading.length
            }
        }
        dict.clear()
        dict.putAll(local)
        entryCount = count
        isLoaded = local.isNotEmpty()
    }

    /** 查询某个读音的候选表记 */
    fun lookup(reading: String): List<Entry> = dict[reading].orEmpty()

    private class Path(val text: String, val rest: Int)

    /**
     * 把整串假名转成日文候选。
     *
     * 返回顺序即优先级：先是最可能的汉字转换，然后是其它写法，
     * 最后补上片假名与平假名（总能让用户原样上屏）。
     */
    fun convert(kana: String, limit: Int = 8): List<Candidate> {
        if (kana.isEmpty() || limit <= 0) return emptyList()
        val out = LinkedHashMap<String, Candidate>(16)

        fun offer(text: String, score: Double, hint: String?) {
            if (text.isEmpty()) return
            if (out.containsKey(text)) return
            out[text] = Candidate(text = text, hint = hint, score = score)
        }

        if (isLoaded) {
            val segments = bestSegments(kana)
            // 1) 整串最优
            val best = segments.joinToString("") { it.first }
            offer(best, 1000.0, labelOf(segments))
            // 2) 第一个文節的其它写法（日文输入法里最常用的「换一个写法」）
            val head = segments.firstOrNull()
            if (head != null && head.second != null && segments.size >= 1) {
                val reading = head.second!!
                val tail = segments.drop(1).joinToString("") { it.first }
                var alt = 0
                for (e in lookup(reading)) {
                    if (alt >= limit) break
                    if (e.surface == head.first) continue
                    offer(e.surface + tail, 900.0 - alt, null)
                    alt++
                }
            }
            // 3) 逐文節换写法：覆盖「读法对、写法不对」以及「整串没转换」的场景。
            //    原样保留的假名段也参与 —— 否则「やま」这种只有单字表记的读音
            //    会一条汉字候选都出不来。
            for (idx in segments.indices) {
                val seg = segments[idx]
                val reading = seg.second ?: seg.first
                val aliases = lookup(reading).filter { it.surface != seg.first }.take(1)
                if (aliases.isEmpty()) continue
                val sb = StringBuilder()
                segments.forEachIndexed { i, s -> sb.append(if (i == idx) aliases[0].surface else s.first) }
                offer(sb.toString(), 700.0 - idx, "第 ${idx + 1} 節")
            }
        }

        // 4) 兜底：一条汉字候选都没有时，先给出原样的假名（= 不做変換），
        //    再补片假名；有汉字候选时片假名紧跟其后，方便外来语改写法。
        if (out.isEmpty()) offer(kana, 350.0, "ひらがな")
        val katakana = RomajiKana.toKatakana(kana)
        offer(katakana, 300.0, "カタカナ")
        if (katakana != kana) offer(kana, 200.0, "ひらがな")

        return out.values.take(limit)
    }

    /** 文節列表 → 候选栏附注（用「＋」连起来，一眼看出切分） */
    private fun labelOf(segments: List<Pair<String, String?>>): String? {
        if (segments.size <= 1) return null
        return segments.joinToString("＋") { it.first }
    }

    /**
     * 动态规划求最低代价切分。
     *
     * 返回 (表记, 该段的假名读音)；读音为 null 表示这一段是原样保留的假名。
     */
    private fun bestSegments(kana: String): List<Pair<String, String?>> {
        val n = kana.length
        val best = DoubleArray(n + 1) { Double.MAX_VALUE }
        val back = IntArray(n + 1) { -1 }
        val surface = arrayOfNulls<String>(n + 1)
        best[0] = 0.0

        for (i in 0 until n) {
            val base = best[i]
            if (base == Double.MAX_VALUE) continue
            // 词表条目：长词优先尝试
            val maxLen = minOf(maxReading, n - i)
            for (len in maxLen downTo 1) {
                val entries = dict[kana.substring(i, i + len)] ?: continue
                for (e in entries) {
                    val cost = base + e.cost + segmentCost
                    if (cost < best[i + len]) {
                        best[i + len] = cost
                        back[i + len] = i
                        surface[i + len] = e.surface
                    }
                }
            }
            // 原样保留一个假名
            val pass = base + passthroughCost
            if (pass < best[i + 1]) {
                best[i + 1] = pass
                back[i + 1] = i
                surface[i + 1] = null
            }
        }

        if (back[n] < 0) return listOf(kana to null)
        val reversed = ArrayList<Pair<String, String?>>(8)
        var p = n
        while (p > 0) {
            val prev = back[p]
            if (prev < 0) break
            val surf = surface[p]
            if (surf != null) {
                reversed.add(surf to kana.substring(prev, p))
            } else {
                // 连续原样保留的假名合并成一段，避免候选栏附注被逐字拆开
                if (reversed.isNotEmpty() && reversed.last().second == null) {
                    reversed[reversed.size - 1] = (kana[prev] + reversed.last().first) to null
                } else {
                    reversed.add(kana[prev].toString() to null)
                }
            }
            p = prev
        }
        reversed.reverse()
        return reversed
    }
}
