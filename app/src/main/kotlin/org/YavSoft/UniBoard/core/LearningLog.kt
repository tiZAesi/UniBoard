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

package org.YavSoft.UniBoard.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 学习记录日志。
 *
 * 记「输入法到底学了什么」：每一次上屏、每一条被自动收录的新词、每次词库导入与
 * AI 补词，都按时间顺序留一条。全部写在应用私有目录 `filesDir/learning_log.tsv`，
 * 环形缓冲最多 [MAX_ENTRIES] 条，不联网、不上报。
 *
 * 为什么单独记一份：用户词频（[org.YavSoft.UniBoard.predict.UserHistory]）是一张
 * 只增不减的 Map，排序不对时很难看出「为什么这个词排前面」。有了带时间戳的流水，
 * 在设置页就能直接读到「刚刚上屏了什么、什么时候被收录」，排查与清理都直观。
 */
class LearningLog(context: Context) {

    /** 一条记录 */
    data class Entry(
        val timeMs: Long,
        /** commit / collect / import / ai */
        val kind: String,
        val text: String,
        val extra: String = ""
    ) {
        /** 给界面用的类别名 */
        val kindLabel: String
            get() = when (kind) {
                KIND_COMMIT -> "上屏"
                KIND_COLLECT -> "收录"
                KIND_IMPORT -> "导入"
                KIND_AI -> "AI"
                else -> kind
            }

        /** 给界面用的时间（只到分钟，够看先后） */
        fun timeLabel(pattern: String = "MM-dd HH:mm"): String =
            SimpleDateFormat(pattern, Locale.US).format(Date(timeMs))
    }

    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    /** 落盘走单线程，别在输入法主线程上写文件 */
    private val io = Executors.newSingleThreadExecutor()
    private val entries = ArrayDeque<Entry>(MAX_ENTRIES)
    private var pendingWrites = 0
    private var dirty = false

    init {
        load()
    }

    private fun load() {
        if (!file.isFile) return
        runCatching {
            file.forEachLine { line ->
                val parts = line.split('\t')
                if (parts.size < 3) return@forEachLine
                val ts = parts[0].toLongOrNull() ?: return@forEachLine
                entries.addLast(Entry(ts, parts[1], parts[2], parts.getOrElse(3) { "" }))
            }
        }
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
    }

    /** 记一条；[text] 里的制表符与换行会被压成空格，免得写坏文件 */
    fun record(kind: String, text: String, extra: String = "") {
        val clean = text.replace('\t', ' ').replace('\n', ' ').trim()
        if (clean.isEmpty()) return
        entries.addLast(Entry(System.currentTimeMillis(), kind, clean.take(120), extra.take(60)))
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
        dirty = true
        pendingWrites++
        if (pendingWrites >= FLUSH_EVERY) flush()
    }

    /** 最近的记录（新的在前） */
    fun recent(limit: Int = 60): List<Entry> =
        entries.descendingIterator().asSequence().take(limit).toList()

    /** 指定类别的记录条数 */
    fun countOf(kind: String): Int = entries.count { it.kind == kind }

    val size: Int get() = entries.size

    fun clear() {
        entries.clear()
        dirty = true
        flushBlocking()
    }

    /** 导出成可读文本（给「导出日志」用） */
    fun exportText(limit: Int = MAX_ENTRIES): String {
        val sb = StringBuilder(4096)
        sb.append("# UniBoard 学习记录日志\n")
        sb.append("# 时间（yyyy-MM-dd HH:mm:ss）\t类别\t内容\t备注\n")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (e in recent(limit).asReversed()) {
            sb.append(fmt.format(Date(e.timeMs))).append('\t')
                .append(e.kindLabel).append('\t')
                .append(e.text)
            if (e.extra.isNotEmpty()) sb.append('\t').append(e.extra)
            sb.append('\n')
        }
        return sb.toString()
    }

    fun flush() {
        if (!dirty) return
        dirty = false
        pendingWrites = 0
        val snapshot = ArrayList(entries)
        io.execute {
            runCatching {
                val sb = StringBuilder(8192)
                for (e in snapshot) {
                    sb.append(e.timeMs).append('\t').append(e.kind).append('\t')
                        .append(e.text)
                    if (e.extra.isNotEmpty()) sb.append('\t').append(e.extra)
                    sb.append('\n')
                }
                file.writeText(sb.toString())
            }
        }
    }

    /** 同步落盘（清空、退出时用，确保立刻生效） */
    fun flushBlocking() {
        dirty = false
        pendingWrites = 0
        val sb = StringBuilder(8192)
        for (e in entries) {
            sb.append(e.timeMs).append('\t').append(e.kind).append('\t').append(e.text)
            if (e.extra.isNotEmpty()) sb.append('\t').append(e.extra)
            sb.append('\n')
        }
        runCatching { file.writeText(sb.toString()) }
    }

    companion object {
        const val KIND_COMMIT = "commit"
        const val KIND_COLLECT = "collect"
        const val KIND_IMPORT = "import"
        const val KIND_AI = "ai"

        private const val FILE_NAME = "learning_log.tsv"
        private const val MAX_ENTRIES = 500
        private const val FLUSH_EVERY = 6
    }
}
