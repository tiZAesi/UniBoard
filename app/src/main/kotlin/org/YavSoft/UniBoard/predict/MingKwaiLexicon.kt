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
 * 明快键盘的字形索引。
 *
 * 参考林语堂 1946 年明快中文打字机的取字方式：**敲两个形状键**——
 * 先按字的上形（本实现用部首近似），再按下形（用 IDS 分解串的末部件）——
 * 机器窗口里随即列出候选，再按号选取。
 *
 * 索引由 [org.YavSoft.UniBoard] 的 `tools/gen_mingkwai.py` 离线生成：
 *
 * ```
 * mingkwai_keys.txt   形状键表      序号<TAB>键面字<TAB>包含的写法
 * mingkwai_index.txt  两键索引      首形序号<TAB>尾形序号<TAB>字:频,字:频,…
 * ```
 *
 * 候选在生成时即按字频降序排好，运行时不再排序，只做切片。
 */
class MingKwaiLexicon {

    data class Shape(val id: Int, val label: String, val members: String)
    data class Word(val text: String, val freq: Int)

    private var shapes: List<Shape> = emptyList()
    private val byPair = HashMap<Int, List<Word>>(1024)
    private val byFirst = HashMap<Int, List<Word>>(64)

    val isLoaded: Boolean get() = shapes.isNotEmpty()

    /** 键盘要显示的 30 个形状键，顺序与索引里的序号一致 */
    val shapeKeys: List<Shape> get() = shapes

    /** 「其他」键的序号（形状键的最后一枚） */
    val otherId: Int get() = if (shapes.isEmpty()) 0 else shapes.size - 1

    fun load(keysReader: BufferedReader, indexReader: BufferedReader) {
        val list = ArrayList<Shape>(32)
        keysReader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val parts = line.split('\t')
            val label = parts.getOrNull(0)?.trim().orEmpty()
            if (label.isEmpty()) return@forEachLine
            list.add(Shape(list.size, label, parts.getOrNull(1)?.trim().orEmpty()))
        }
        shapes = list

        byPair.clear()
        byFirst.clear()
        val merged = HashMap<Int, ArrayList<Word>>(64)
        indexReader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val parts = line.split('\t')
            if (parts.size < 3) return@forEachLine
            val a = parts[0].toIntOrNull() ?: return@forEachLine
            val b = parts[1].toIntOrNull() ?: return@forEachLine
            val words = ArrayList<Word>(24)
            for (item in parts[2].split(',')) {
                val sp = item.lastIndexOf(':')
                if (sp <= 0) continue
                val text = item.substring(0, sp)
                if (text.length != 1) continue
                words.add(Word(text, item.substring(sp + 1).toIntOrNull() ?: 0))
            }
            if (words.isEmpty()) return@forEachLine
            byPair[a * 64 + b] = words
            // 首形单独成桶：用户只记得上形、想不起下形时仍有候选可挑
            val acc = merged.getOrPut(a) { ArrayList(64) }
            if (acc.size < 120) acc.addAll(words.take(24))
        }
        for ((k, v) in merged) {
            byFirst[k] = v.sortedByDescending { it.freq }.take(60)
        }
    }

    /** 两键定位 */
    fun lookup(first: Int, second: Int, limit: Int = 24): List<Word> =
        byPair[first * 64 + second]?.take(limit) ?: emptyList()

    /** 只按了上形：给出该形状下最常用的一批字 */
    fun lookupFirst(first: Int, limit: Int = 24): List<Word> =
        byFirst[first]?.take(limit) ?: emptyList()

    fun labelOf(id: Int): String = shapes.getOrNull(id)?.label.orEmpty()
}
