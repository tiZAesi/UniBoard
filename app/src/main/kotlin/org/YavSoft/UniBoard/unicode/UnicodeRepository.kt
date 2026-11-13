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

package org.YavSoft.UniBoard.unicode

import android.icu.lang.UCharacter
import android.icu.lang.UCharacterCategory

/**
 * Unicode 数据访问层：码点解析、字符名、区块、分类与名称检索。
 *
 * 所有数据来自系统内置的 ICU 数据库，不依赖网络。
 */
object UnicodeRepository {

    /**
     * 区块索引。
     *
     * 覆盖范围按 Unicode 17.0.0（2025-09）的已编码区块整理，按主题分组，
     * 面板上的「索引」按本表浏览。注意：**甲骨文、女真文目前尚未编码**，
     * 仍停留在提案 / WG2 讨论阶段，没有官方码点，因此这里不会出现。
     *
     * 码点即使在本表里，能否显示仍取决于系统字体；
     * 面板会把缺字形的字符照常列出，只是字面上会是豆腐块。
     */
    data class Block(val name: String, val start: Int, val end: Int, val group: String)

    val BLOCKS: List<Block> = listOf(
        // —— 拉丁与欧洲 ——
        Block("基本拉丁", 0x0020, 0x007E, "拉丁与欧洲"),
        Block("拉丁补充", 0x00A0, 0x00FF, "拉丁与欧洲"),
        Block("拉丁扩展A", 0x0100, 0x017F, "拉丁与欧洲"),
        Block("拉丁扩展B", 0x0180, 0x024F, "拉丁与欧洲"),
        Block("国际音标", 0x0250, 0x02AF, "拉丁与欧洲"),
        Block("组合附加符号", 0x0300, 0x036F, "拉丁与欧洲"),
        Block("希腊字母", 0x0391, 0x03C9, "拉丁与欧洲"),
        Block("西里尔字母", 0x0410, 0x044F, "拉丁与欧洲"),
        Block("亚美尼亚文", 0x0531, 0x058A, "拉丁与欧洲"),
        Block("希伯来文", 0x05D0, 0x05EA, "拉丁与欧洲"),
        Block("欧甘文", 0x1680, 0x169F, "拉丁与欧洲"),
        Block("卢恩文", 0x16A0, 0x16FF, "拉丁与欧洲"),
        Block("提非纳文", 0x2D30, 0x2D7F, "拉丁与欧洲"),

        // —— 中东与南亚 ——
        Block("阿拉伯文", 0x0621, 0x064A, "中东与南亚"),
        Block("叙利亚文", 0x0700, 0x074F, "中东与南亚"),
        Block("天城文", 0x0900, 0x097F, "中东与南亚"),
        Block("孟加拉文", 0x0980, 0x09FF, "中东与南亚"),
        Block("泰米尔文", 0x0B80, 0x0BFF, "中东与南亚"),
        Block("泰卢固文", 0x0C00, 0x0C7F, "中东与南亚"),
        Block("泰文", 0x0E01, 0x0E3A, "中东与南亚"),
        Block("老挝文", 0x0E81, 0x0EDF, "中东与南亚"),
        Block("藏文", 0x0F00, 0x0FFF, "中东与南亚"),
        Block("缅甸文", 0x1000, 0x109F, "中东与南亚"),
        Block("高棉文", 0x1780, 0x17FF, "中东与南亚"),

        // —— 东亚与汉字 ——
        Block("中日韩符号", 0x3000, 0x303F, "东亚与汉字"),
        Block("平假名", 0x3041, 0x3096, "东亚与汉字"),
        Block("片假名", 0x30A1, 0x30FA, "东亚与汉字"),
        Block("注音符号", 0x3105, 0x312F, "东亚与汉字"),
        Block("韩文兼容字母", 0x3131, 0x318E, "东亚与汉字"),
        Block("康熙部首", 0x2F00, 0x2FD5, "东亚与汉字"),
        Block("部首补充", 0x2E80, 0x2EFF, "东亚与汉字"),
        Block("中日韩扩展A", 0x3400, 0x4DBF, "东亚与汉字"),
        Block("中日韩统一表意", 0x4E00, 0x9FA5, "东亚与汉字"),
        Block("彝文", 0xA000, 0xA48C, "东亚与汉字"),
        Block("韩文音节", 0xAC00, 0xD7A3, "东亚与汉字"),
        Block("兼容汉字", 0xF900, 0xFA6A, "东亚与汉字"),
        Block("兼容汉字补充", 0x2F800, 0x2FA1D, "东亚与汉字"),
        Block("中日韩扩展B", 0x20000, 0x2A6D6, "东亚与汉字"),
        Block("中日韩扩展C", 0x2A700, 0x2B739, "东亚与汉字"),
        Block("中日韩扩展D", 0x2B740, 0x2B81D, "东亚与汉字"),
        Block("中日韩扩展E", 0x2B820, 0x2CEA1, "东亚与汉字"),
        Block("中日韩扩展F", 0x2CEB0, 0x2EBE0, "东亚与汉字"),
        Block("中日韩扩展I", 0x2EBF0, 0x2EE5D, "东亚与汉字"),
        Block("中日韩扩展J", 0x323B0, 0x3347F, "东亚与汉字"),

        // —— 古文字与已消亡文字 ——
        Block("线形文字B", 0x10000, 0x1007F, "古文字"),
        Block("古意大利文", 0x10300, 0x1032F, "古文字"),
        Block("哥特文", 0x10330, 0x1034F, "古文字"),
        Block("古波斯文", 0x103A0, 0x103DF, "古文字"),
        Block("乌加里特文", 0x10380, 0x1039F, "古文字"),
        Block("古突厥文", 0x10C00, 0x10C4F, "古文字"),
        Block("古匈牙利文", 0x10C80, 0x10CFF, "古文字"),
        Block("回鹘文", 0x10F70, 0x10FAF, "古文字"),
        Block("楔形文字", 0x12000, 0x123FF, "古文字"),
        Block("埃及圣书体", 0x13000, 0x1342F, "古文字"),
        Block("安纳托利亚象形文字", 0x14400, 0x1467F, "古文字"),
        Block("西夏文", 0x17000, 0x187F7, "古文字"),
        Block("西夏文部首", 0x18800, 0x18AFF, "古文字"),
        Block("契丹小字", 0x18B00, 0x18CFF, "古文字"),
        Block("女书", 0x1B170, 0x1B2FB, "古文字"),
        Block("八思巴文", 0xA840, 0xA877, "古文字"),
        Block("蒙古文", 0x1800, 0x18AF, "古文字"),

        // —— 数学、音乐与符号 ——
        Block("通用标点", 0x2010, 0x205E, "符号"),
        Block("上下标", 0x2070, 0x209C, "符号"),
        Block("货币符号", 0x20A0, 0x20BF, "符号"),
        Block("字母式符号", 0x2100, 0x214F, "符号"),
        Block("数字形式", 0x2150, 0x218F, "符号"),
        Block("箭头", 0x2190, 0x21FF, "符号"),
        Block("数学运算符", 0x2200, 0x22FF, "符号"),
        Block("技术符号", 0x2300, 0x23FF, "符号"),
        Block("制表符", 0x2500, 0x257F, "符号"),
        Block("几何图形", 0x25A0, 0x25FF, "符号"),
        Block("杂项符号", 0x2600, 0x26FF, "符号"),
        Block("装饰符号", 0x2700, 0x27BF, "符号"),
        Block("盲文", 0x2800, 0x28FF, "符号"),
        Block("数学字母数字", 0x1D400, 0x1D7FF, "符号"),
        Block("音乐符号", 0x1D100, 0x1D1FF, "符号"),
        Block("麻将牌", 0x1F000, 0x1F02F, "符号"),
        Block("扑克牌", 0x1F0A0, 0x1F0FF, "符号"),

        // —— 表情与图形 ——
        Block("表情符号", 0x1F300, 0x1F5FF, "表情与图形"),
        Block("表情脸谱", 0x1F600, 0x1F64F, "表情与图形"),
        Block("交通标志", 0x1F680, 0x1F6FF, "表情与图形"),
        Block("补充符号", 0x1F900, 0x1F9FF, "表情与图形"),
        Block("扩展表情A", 0x1FA70, 0x1FAFF, "表情与图形"),
        Block("私用区", 0xE000, 0xF8FF, "表情与图形")
    )

    /** 分组顺序（面板索引按此分组显示） */
    val GROUPS: List<String> = BLOCKS.map { it.group }.distinct()

    fun blocksInGroup(group: String): List<Block> = BLOCKS.filter { it.group == group }

    /** 解析用户输入的码点写法，支持 4E00 / U+4E00 / 0x4E00 / &#x4E00; / &#19968; / 单字符 */
    fun parseCodePoint(raw: String): Int? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        if (s.length == 1 || (s.codePointCount(0, s.length) == 1 && s.codePointAt(0) > 0x7F)) {
            return s.codePointAt(0)
        }
        var body = s
        if (body.startsWith("&#x", true) || body.startsWith("&#X")) {
            body = body.removePrefix("&#x").removePrefix("&#X").removeSuffix(";")
            return body.toIntOrNull(16)
        }
        if (body.startsWith("&#")) {
            body = body.removePrefix("&#").removeSuffix(";")
            return body.toIntOrNull(10)
        }
        for (prefix in listOf("U+", "u+", "0x", "0X", "\\u", "\\\\u")) {
            if (body.startsWith(prefix)) {
                body = body.substring(prefix.length)
                break
            }
        }
        val v = body.toIntOrNull(16) ?: return null
        if (v < 0 || v > 0x10FFFF) return null
        if (v in 0xD800..0xDFFF) return null
        return v
    }

    /** 字符的 Unicode 名称，无名称时返回空串 */
    fun nameOf(codePoint: Int): String {
        if (codePoint < 0 || codePoint > 0x10FFFF) return ""
        return runCatching { UCharacter.getName(codePoint) }.getOrNull().orEmpty()
    }

    /** 通用类别名称（如“字母”“标点”） */
    fun categoryName(codePoint: Int): String {
        // UCharacterCategory 的常量在 Android 里是 byte，统一转成 Byte 再比较
        val cat = runCatching { UCharacter.getType(codePoint) }.getOrDefault(0).toByte()
        return when (cat) {
            UCharacterCategory.UPPERCASE_LETTER, UCharacterCategory.LOWERCASE_LETTER,
            UCharacterCategory.TITLECASE_LETTER, UCharacterCategory.MODIFIER_LETTER,
            UCharacterCategory.OTHER_LETTER -> "字母"
            UCharacterCategory.DECIMAL_DIGIT_NUMBER, UCharacterCategory.LETTER_NUMBER,
            UCharacterCategory.OTHER_NUMBER -> "数字"
            UCharacterCategory.SPACE_SEPARATOR, UCharacterCategory.LINE_SEPARATOR,
            UCharacterCategory.PARAGRAPH_SEPARATOR -> "空白"
            UCharacterCategory.CONTROL -> "控制符"
            UCharacterCategory.FORMAT -> "格式符"
            UCharacterCategory.PRIVATE_USE -> "私用区"
            UCharacterCategory.SURROGATE -> "代理项"
            UCharacterCategory.DASH_PUNCTUATION, UCharacterCategory.START_PUNCTUATION,
            UCharacterCategory.END_PUNCTUATION, UCharacterCategory.CONNECTOR_PUNCTUATION,
            UCharacterCategory.OTHER_PUNCTUATION, UCharacterCategory.INITIAL_PUNCTUATION,
            UCharacterCategory.FINAL_PUNCTUATION -> "标点"
            UCharacterCategory.MATH_SYMBOL, UCharacterCategory.CURRENCY_SYMBOL,
            UCharacterCategory.MODIFIER_SYMBOL, UCharacterCategory.OTHER_SYMBOL -> "符号"
            UCharacterCategory.UNASSIGNED -> "未分配"
            else -> "其他"
        }
    }

    /** 码点转字符串（正确处理代理对） */
    fun charOf(codePoint: Int): String {
        if (codePoint < 0 || codePoint > 0x10FFFF) return ""
        return String(Character.toChars(codePoint))
    }

    /** 十六进制展示，如 U+4E00 */
    fun hexLabel(codePoint: Int): String = "U+" + codePoint.toString(16).uppercase().padStart(4, '0')

    /** 找到码点所属的内置区块 */
    fun blockOf(codePoint: Int): Block? = BLOCKS.firstOrNull { codePoint in it.start..it.end }

    private val blockCache = HashMap<Int, List<Int>>(16)

    /** 列出某个区块内的有效码点（结果缓存，重复切换区块不再重复扫描） */
    fun charsInBlock(block: Block): List<Int> {
        blockCache[block.start]?.let { return it }
        val out = ArrayList<Int>(block.end - block.start + 1)
        for (cp in block.start..block.end) {
            if (cp in 0xD800..0xDFFF) continue
            if (!UCharacter.isDefined(cp)) continue
            out.add(cp)
        }
        blockCache[block.start] = out
        return out
    }

    /** 按 Unicode 名称检索字符（耗时，请在后台线程调用） */
    fun searchByName(query: String, limit: Int = 120): List<Int> {
        val q = query.trim().uppercase()
        if (q.length < 2) return emptyList()
        val out = ArrayList<Int>(limit)
        for (cp in 0x20..0xD7FF) {
            val name = nameOf(cp)
            if (name.isNotEmpty() && name.contains(q)) {
                out.add(cp)
                if (out.size >= limit) return out
            }
        }
        for (cp in 0xE000..0x2FFFF) {
            val name = nameOf(cp)
            if (name.isNotEmpty() && name.contains(q)) {
                out.add(cp)
                if (out.size >= limit) return out
            }
        }
        return out
    }

    /** 检索提示关键词 */
    val SAMPLE_QUERIES: List<String> = listOf(
        "SNOWMAN", "HEART", "ARROW", "MUSICAL", "GREEK", "CHESS", "STAR", "BOX"
    )

    /** 码点输入容错：把常见误输字母映射成数字后重解析 */
    fun correctCodePoint(raw: String): Int? {
        var s = raw.trim().lowercase()
        if (s.isEmpty()) return null
        for (p in listOf("u+", "0x", "&#x", "&#", "\\u")) {
            if (s.startsWith(p)) s = s.substring(p.length)
        }
        s = s.replace("[", "").replace("]", "").replace(";", "").replace(" ", "")
        if (s.isEmpty()) return null
        val map = mapOf('o' to '0', 'q' to '0', 'i' to '1', 'l' to '1',
            'z' to '2', 's' to '5', 'g' to '6', 't' to '7', 'b' to '8')
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(map[c] ?: c)
        val hex = sb.toString()
        if (hex.isEmpty() || hex.length > 6) return null
        if (!hex.all { it in "0123456789abcdef" }) return null
        val v = hex.toIntOrNull(16) ?: return null
        if (v in 0xD800..0xDFFF || v > 0x10FFFF) return null
        return v
    }

    /**
     * 码点前缀联想：输入 `4E0` 就能看到 `4E00–4E0F` 区间的字符。
     *
     * 原实现把前缀当成「高位」再左移（`base shl (digits*4)`），三位前缀会移到
     * 0x4E0000 直接越界返回空，所以这里改成按目标码长（4/5/6 位）补齐低位。
     */
    fun suggestByPrefix(prefix: String, limit: Int = 240): List<Int> {
        val hex = prefix.trim().lowercase()
        if (hex.isEmpty() || hex.length > 5) return emptyList()
        if (!hex.all { it in "0123456789abcdef" }) return emptyList()
        val base = hex.toLong(16).toInt()
        val out = LinkedHashSet<Int>(minOf(limit, 256))
        for (target in intArrayOf(4, 5, 6)) {
            if (target < hex.length) continue
            val remain = target - hex.length
            if (remain > 3) continue
            val count = 1 shl (4 * remain)
            val start = base shl (4 * remain)
            for (i in 0 until count) {
                val cp = start + i
                if (cp > 0x10FFFF) break
                if (cp in 0xD800..0xDFFF) continue
                if (!UCharacter.isDefined(cp)) continue
                out.add(cp)
                if (out.size >= limit) return out.toList()
            }
        }
        return out.toList()
    }
}
