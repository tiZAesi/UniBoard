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

package org.YavSoft.UniBoard.clip

import android.content.Context
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * 常用语：手机上高频输入的整句（地址、邮箱、自我介绍等），在剪贴板面板里一键插入。
 *
 * 除原文之外还维护两份附加信息：
 * - 模板变量：`{date}` `{time}` `{datetime}` `{clip}`，插入时展开成当天日期、当前时间与当前剪贴板；
 * - 使用统计：按「应用包名 + 常用语」记次数，面板据此把最近在同一个应用里用过的条目
 *   单独归到「本应用」分组，减少滚动查找。
 */
class QuickPhrases(context: Context) {

    private val file = File(context.applicationContext.filesDir, "quick_phrases.txt")
    private val usageFile = File(context.applicationContext.filesDir, "quick_phrase_usage.txt")
    private val items = ArrayList<String>()

    /** key = "包名|常用语hashCode"，value = 使用次数 */
    private val usage = HashMap<String, Int>()

    init {
        load()
        loadUsage()
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            file.readLines().forEach { line ->
                val t = line.replace("\\n", "\n")
                if (t.isNotBlank()) items.add(t)
            }
        }
    }

    private fun loadUsage() {
        if (!usageFile.exists()) return
        runCatching {
            usageFile.readLines().forEach { line ->
                val parts = line.split('\t')
                if (parts.size != 3) return@forEach
                val count = parts[2].toIntOrNull() ?: return@forEach
                usage[parts[0] + "|" + parts[1]] = count
            }
        }
    }

    fun list(): List<String> = items.toList()

    fun contains(text: String): Boolean = items.contains(text)

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        items.remove(t)
        items.add(0, t)
        persist()
    }

    fun remove(text: String) {
        if (items.remove(text)) persist()
    }

    fun clear() {
        items.clear()
        usage.clear()
        persist()
        runCatching { usageFile.delete() }
    }

    /**
     * 展开模板变量。`clip` 传入当前剪贴板文本，没有则保留 `{clip}` 原样。
     */
    fun render(text: String, clip: String?): String {
        if (text.indexOf('{') < 0) return text
        val now = Calendar.getInstance()
        val date = String.format(
            Locale.US, "%04d-%02d-%02d",
            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)
        )
        val time = String.format(
            Locale.US, "%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)
        )
        var out = text
            .replace("{date}", date)
            .replace("{time}", time)
            .replace("{datetime}", "$date $time")
        if (!clip.isNullOrEmpty()) out = out.replace("{clip}", clip)
        return out
    }

    /** 记录一次插入，按当前应用累加 */
    fun noteUse(pkg: String?, text: String) {
        if (pkg.isNullOrEmpty() || text.isEmpty()) return
        val key = pkg + "|" + text.hashCode()
        usage[key] = (usage[key] ?: 0) + 1
        persistUsage()
    }

    /** 某个应用下用得最多的若干条常用语 */
    fun recentForPackage(pkg: String?, limit: Int = 4): List<String> {
        if (pkg.isNullOrEmpty() || usage.isEmpty()) return emptyList()
        val prefix = pkg + "|"
        val hits = usage.entries
            .filter { it.key.startsWith(prefix) }
            .sortedByDescending { it.value }
            .mapNotNull { entry ->
                val hash = entry.key.substring(prefix.length).toIntOrNull() ?: return@mapNotNull null
                items.firstOrNull { it.hashCode() == hash }
            }
        return hits.distinct().take(limit)
    }

    private fun persist() {
        runCatching {
            file.writeText(items.joinToString("\n") { it.replace("\n", "\\n") })
        }
    }

    private fun persistUsage() {
        runCatching {
            usageFile.writeText(
                usage.entries.joinToString("\n") { (k, v) ->
                    val bar = k.indexOf('|')
                    if (bar <= 0) k else k.substring(0, bar) + "\t" + k.substring(bar + 1) + "\t" + v
                }
            )
        }
    }
}
