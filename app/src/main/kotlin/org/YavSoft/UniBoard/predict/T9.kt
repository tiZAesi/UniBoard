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
 * 九键（T9）数字映射。
 *
 * 传统功能机上，2–9 八个数字键各自承担三到四个字母：
 * 2→abc、3→def、4→ghi、5→jkl、6→mno、7→pqrs、8→tuv、9→wxyz。
 * 输入一个汉字的拼音时按对应的数字序列，词库侧再把拼音串折算成同样的数字序列做匹配，
 * 于是「94664」（zhong）能匹配到「zhong」这一组词。
 *
 * 所有折算都走这里，避免键盘、词库、联想三处各写一遍映射表。
 */
object T9 {

    /** 拼音串 → 数字串；非 a–z 字符（ü、空格、撇号）按 ü→v 或其他规则忽略 */
    fun digits(pinyin: String): String {
        val sb = StringBuilder(pinyin.length)
        for (ch in pinyin.lowercase()) {
            when (ch) {
                in 'a'..'c' -> sb.append('2')
                in 'd'..'f' -> sb.append('3')
                in 'g'..'i' -> sb.append('4')
                in 'j'..'l' -> sb.append('5')
                in 'm'..'o' -> sb.append('6')
                in 'p'..'s' -> sb.append('7')
                in 't'..'v' -> sb.append('8')
                in 'w'..'z' -> sb.append('9')
                'ü' -> sb.append('8')   // ü 归入 v 所在的 8 键
                else -> Unit
            }
        }
        return sb.toString()
    }
}
