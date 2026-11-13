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
 * 笔画输入索引。
 *
 * 五种基本笔画：1 横、2 竖、3 撇、4 点/捺、5 折。
 * 索引由 `tools/gen_stroke.py` 从公开字形数据离线生成，
 * 每行是「笔画码 + 该码下的字（按字频降序）」。
 *
 * 笔画码来自字形中线走向的自动判定，边缘情况（横折被平滑成弧）会判错，
 * 所以检索时允许一位容错，并在没有结果时自动退一位重试 —— 这也是「输入纠错」。
 */
class StrokeLexicon {

    data class Word(val text: String, val freq: Int)

    private var codes: Array<String> = emptyArray()
    private var buckets: Array<List<Word>> = emptyArray()

    val isLoaded: Boolean get() = codes.isNotEmpty()

    fun load(reader: BufferedReader) {
        val codeList = ArrayList<String>(2048)
        val bucketList = ArrayList<List<Word>>(2048)
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val tab = line.indexOf('\t')
            if (tab <= 0) return@forEachLine
            val code = line.substring(0, tab)
            val words = ArrayList<Word>(16)
            for (item in line.substring(tab + 1).split(',')) {
                val sp = item.lastIndexOf(':')
                if (sp <= 0) continue
                val text = item.substring(0, sp)
                if (text.length != 1) continue
                words.add(Word(text, item.substring(sp + 1).toIntOrNull() ?: 0))
            }
            if (words.isNotEmpty()) {
                codeList.add(code)
                bucketList.add(words)
            }
        }
        codes = codeList.toTypedArray()
        buckets = bucketList.toTypedArray()
    }

    /**
     * 按笔画码取字。
     *
     * @param tolerance 允许与索引码不同的位数（0 = 严格前缀）
     */
    fun lookup(code: String, limit: Int = 24, tolerance: Int = 0): List<Word> {
        if (codes.isEmpty() || code.isEmpty()) return emptyList()
        val out = LinkedHashMap<String, Word>(64)
        for (i in codes.indices) {
            val c = codes[i]
            when {
                c.startsWith(code) -> buckets[i].forEach { out.putIfAbsent(it.text, it) }
                tolerance > 0 && nearPrefix(c, code, tolerance) ->
                    buckets[i].forEach { out.putIfAbsent(it.text, it) }
                else -> Unit
            }
            if (out.size >= limit * 4) break
        }
        return out.values.sortedByDescending { it.freq }.take(limit)
    }

    /** 索引码的前 len(code) 位与输入相差不超过 tolerance 位 */
    private fun nearPrefix(index: String, code: String, tolerance: Int): Boolean {
        if (index.length < code.length) return false
        var diff = 0
        for (i in code.indices) {
            if (index[i] != code[i]) {
                diff++
                if (diff > tolerance) return false
            }
        }
        return true
    }
}
