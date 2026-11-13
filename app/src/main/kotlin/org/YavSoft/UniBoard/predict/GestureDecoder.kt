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
 * 滑行输入（glide typing）解码器。
 *
 * 键盘侧只负责把手指一路划过的字母键按顺序交上来（相邻重复已去掉），
 * 这里再从句库里挑出「按顺序包含这些字母」的词。
 *
 * 之所以用子序列匹配而不是轨迹距离模型：
 * - 不依赖每个键的坐标，横屏、不同密度、不同键盘高度都不用重新标定；
 * - 多划到几个键、少划几个键都能容忍，用户在真机上更容易命中；
 * - 纯字符串比较，一万多词的库一次解码在毫秒级完成。
 *
 * 代价是短词（2–3 个字母）区分度低，所以长度差超过 [LEN_TOLERANCE] 的词直接排除，
 * 且要求首尾字母与划过的首尾键一致 —— 这两条把误判压下来大半。
 */
object GestureDecoder {

    private const val LEN_TOLERANCE = 3

    /**
     * @param stroke  手指经过的字母序列（小写，相邻去重）
     * @param lexicon 英文词库
     * @param limit   返回候选数
     */
    fun decode(stroke: List<String>, lexicon: EnglishLexicon, limit: Int = 8): List<Candidate> {
        val letters = normalize(stroke)
        if (letters.length < 2 || !lexicon.isLoaded) return emptyList()

        val first = letters[0]
        val last = letters[letters.length - 1]
        val out = ArrayList<Candidate>(16)

        lexicon.forEachWord { word, freq ->
            val w = word.lowercase()
            if (w.length < 2) return@forEachWord
            if (w.length < letters.length - LEN_TOLERANCE) return@forEachWord
            if (w.length > letters.length + LEN_TOLERANCE) return@forEachWord
            if (w[0] != first || w[w.length - 1] != last) return@forEachWord
            if (!isSubsequence(w, letters)) return@forEachWord

            val lengthGap = kotlin.math.abs(w.length - letters.length)
            val score = kotlin.math.log10(freq.toDouble() + 10.0) * 10.0 +
                40.0 - lengthGap * 8.0
            out.add(Candidate(text = w, score = score))
        }

        return out.sortedByDescending { it.score }.take(limit)
    }

    private fun normalize(stroke: List<String>): String {
        val sb = StringBuilder(stroke.size)
        for (s in stroke) {
            if (s.length != 1) continue
            val ch = s[0].lowercaseChar()
            if (ch !in 'a'..'z') continue
            if (sb.isNotEmpty() && sb[sb.length - 1] == ch) continue
            sb.append(ch)
        }
        return sb.toString()
    }

    /** word 的字母能否按顺序在 stroke 中找到 */
    private fun isSubsequence(word: String, stroke: String): Boolean {
        var i = 0
        for (ch in stroke) {
            if (i < word.length && word[i] == ch) i++
            if (i == word.length) return true
        }
        return false
    }
}
