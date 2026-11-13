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

/** 英文词表：前缀树 + 词频，用于联想与拼写纠错 */
class EnglishLexicon {

    data class Word(val text: String, val freq: Int)

    private class Node {
        val children = HashMap<Char, Node>(4)
        var word: String? = null
        var freq: Int = 0
    }

    private val root = Node()
    private var wordCount = 0

    val size: Int get() = wordCount
    val isLoaded: Boolean get() = wordCount > 0

    fun load(reader: BufferedReader) {
        reader.forEachLine { line ->
            if (line.isEmpty()) return@forEachLine
            val tab = line.indexOf('\t')
            val w = if (tab > 0) line.substring(0, tab) else line
            val f = if (tab > 0) line.substring(tab + 1).toIntOrNull() ?: 0 else 0
            if (w.isEmpty()) return@forEachLine
            insert(w.lowercase(), f)
        }
    }

    private fun insert(word: String, freq: Int) {
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { Node() }
        }
        if (node.word == null) wordCount++
        node.word = word
        node.freq = freq
    }

    private var sigWords: Array<String> = emptyArray()
    private var sigOrder: IntArray = IntArray(0)
    private var sigFreq: IntArray = IntArray(0)

    /** 九键数字签名：abc→2 … wxyz→9 */
    private fun signature(word: String): String {
        val sb = StringBuilder(word.length)
        for (ch in word) {
            when (ch) {
                in 'a'..'c' -> sb.append('2')
                in 'd'..'f' -> sb.append('3')
                in 'g'..'i' -> sb.append('4')
                in 'j'..'l' -> sb.append('5')
                in 'm'..'o' -> sb.append('6')
                in 'p'..'s' -> sb.append('7')
                in 't'..'v' -> sb.append('8')
                in 'w'..'z' -> sb.append('9')
                else -> Unit
            }
        }
        return sb.toString()
    }

    private fun buildSignatureIndex() {
        val words = ArrayList<String>(wordCount)
        val freqs = ArrayList<Int>(wordCount)
        forEachWord { w, f ->
            words.add(w)
            freqs.add(f)
        }
        sigWords = words.toTypedArray()
        sigFreq = freqs.toIntArray()
        val sigs = Array(sigWords.size) { signature(sigWords[it]) }
        sigOrder = sigs.indices.sortedBy { sigs[it] }.toIntArray()
        sigCache = sigs
    }

    private var sigCache: Array<String> = emptyArray()

    /** 英文九键：数字串前缀匹配，按词频降序 */
    fun t9Prefix(digits: String, limit: Int = 24): List<Word> {
        if (wordCount == 0 || digits.isEmpty()) return emptyList()
        if (sigWords.isEmpty()) buildSignatureIndex()
        val out = ArrayList<Word>(limit)
        var i = lowerBoundSig(digits)
        while (i < sigOrder.size && out.size < limit) {
            val idx = sigOrder[i]
            val sig = sigCache[idx]
            if (!sig.startsWith(digits)) break
            out.add(Word(sigWords[idx], sigFreq[idx]))
            i++
        }
        return out.sortedByDescending { it.freq }.take(limit)
    }

    private fun lowerBoundSig(target: String): Int {
        var lo = 0
        var hi = sigOrder.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sigCache[sigOrder[mid]] < target) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** 遍历全部词条（滑行输入解码用；按字典序，不保证顺序语义） */
    fun forEachWord(action: (String, Int) -> Unit) {
        if (wordCount == 0) return
        val stack = ArrayDeque<Node>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            node.word?.let { action(it, node.freq) }
            for (child in node.children.values) stack.addLast(child)
        }
    }

    /** 前缀联想：返回以 prefix 开头的词，按词频降序 */
    fun prefix(prefix: String, limit: Int = 24): List<Word> {
        if (wordCount == 0) return emptyList()
        val p = prefix.lowercase()
        var node = root
        for (ch in p) {
            node = node.children[ch] ?: return emptyList()
        }
        val out = ArrayList<Word>(limit * 2)
        collect(node, out, limit * 4)
        return out.sortedByDescending { it.freq }.take(limit)
    }

    private fun collect(node: Node, out: MutableList<Word>, cap: Int) {
        if (out.size >= cap) return
        val w = node.word
        if (w != null) out.add(Word(w, node.freq))
        for (child in node.children.values) {
            collect(child, out, cap)
            if (out.size >= cap) return
        }
    }

    /** 遍历全部词条（供拼写纠错使用，调用方缓存结果） */
    fun allWords(limit: Int = 30000): List<Word> {
        val out = ArrayList<Word>(limit)
        collect(root, out, limit)
        return out
    }

    fun contains(word: String): Boolean {
        var node = root
        for (ch in word.lowercase()) {
            node = node.children[ch] ?: return false
        }
        return node.word != null
    }
}
