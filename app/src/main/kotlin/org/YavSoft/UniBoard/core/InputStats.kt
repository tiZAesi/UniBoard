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
import java.util.Date
import java.util.Locale

/**
 * 输入统计（全部只存在本机应用私有目录，不联网、不上报）。
 *
 * 只记四件事：累计上屏字数、今日字数、按键次数、上屏候选次数。
 * 数据落在 `filesDir/input_stats.txt`，格式是极简的 `key=value`，
 * 坏行直接忽略 —— 统计坏了不该影响打字。
 *
 * 写盘刻意做成「攒够 [FLUSH_EVERY] 次再写」：输入法每敲一个键都写一次文件
 * 会明显拖慢候选刷新，而统计这种数据丢最后几个键完全无所谓。
 */
class InputStats(context: Context) {

    /** 统计快照，给设置页展示 */
    data class Snapshot(
        val totalChars: Int,
        val todayChars: Int,
        val keys: Int,
        val commits: Int,
        val today: String
    )

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val today: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private var totalChars: Int = 0
    private var todayChars: Int = 0
    private var storedToday: String = today
    private var keys: Int = 0
    private var commits: Int = 0
    private var pendingWrites: Int = 0
    private var dirty: Boolean = false

    init {
        load()
    }

    private fun load() {
        if (!file.isFile) return
        runCatching {
            file.forEachLine { line ->
                val eq = line.indexOf('=')
                if (eq <= 0) return@forEachLine
                val key = line.substring(0, eq)
                val value = line.substring(eq + 1).trim().toIntOrNull() ?: return@forEachLine
                when (key) {
                    "total_chars" -> totalChars = value
                    "today_chars" -> todayChars = value
                    "keys" -> keys = value
                    "commits" -> commits = value
                    "today" -> storedToday = line.substring(eq + 1).trim()
                    else -> Unit
                }
            }
        }
        if (storedToday != today) {
            // 跨天：今日字数从零开始
            todayChars = 0
            storedToday = today
        }
    }

    /** 记录一次按键 */
    fun recordKey() {
        keys++
        markDirty()
    }

    /** 记录一次上屏（只数非空白字符，标点算一个） */
    fun recordCommit(text: String) {
        var count = 0
        for (ch in text) if (!ch.isWhitespace()) count++
        if (count <= 0) return
        commits++
        totalChars += count
        if (storedToday == today) todayChars += count else {
            storedToday = today
            todayChars = count
        }
        markDirty()
    }

    private fun markDirty() {
        dirty = true
        pendingWrites++
        if (pendingWrites >= FLUSH_EVERY) flush()
    }

    /** 落盘（攒够次数、输入法隐藏或进程结束时调用） */
    fun flush() {
        if (!dirty) return
        dirty = false
        pendingWrites = 0
        runCatching {
            file.writeText(
                buildString {
                    appendLine("total_chars=$totalChars")
                    appendLine("today_chars=$todayChars")
                    appendLine("keys=$keys")
                    appendLine("commits=$commits")
                    appendLine("today=$storedToday")
                }
            )
        }
    }

    fun snapshot(): Snapshot = Snapshot(
        totalChars = totalChars,
        todayChars = if (storedToday == today) todayChars else 0,
        keys = keys,
        commits = commits,
        today = today
    )

    /** 清空统计 */
    fun reset() {
        totalChars = 0
        todayChars = 0
        keys = 0
        commits = 0
        storedToday = today
        dirty = true
        flush()
    }

    private companion object {
        const val FILE_NAME = "input_stats.txt"
        const val FLUSH_EVERY = 24
    }
}
