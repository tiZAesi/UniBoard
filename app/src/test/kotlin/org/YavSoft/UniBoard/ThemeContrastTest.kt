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

package org.YavSoft.UniBoard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.YavSoft.UniBoard.ui.theme.KeyboardThemes

/**
 * 深浅色可读性回归测试。
 *
 * 对应用户报的那个问题：**主题跟随系统、系统是深色，键盘却是浅底白字**。
 * 修法不是「猜哪套配色对」，而是在解析完配色后做一次验收：
 * 底色必须落在模式正确的一侧，正文与底色的对比度必须够。
 * 这里的用例就是钉住这条验收规则。
 */
class ThemeContrastTest {

    /** 内置浅色 / 深色调色板的取值（与 KeyboardTheme.kt 里的静态调色板一致） */
    private val lightBg = 0xFFE7E9EE.toInt()
    private val lightText = 0xFF1B1C1E.toInt()
    private val darkBg = 0xFF141518.toInt()
    private val darkText = 0xFFE7E8EA.toInt()
    private val pureBlackBg = 0xFF000000.toInt()

    @Test
    fun `内置调色板在各自模式下都合格`() {
        assertTrue("浅色：浅底深字", KeyboardThemes.readableEnough(lightBg, lightText, dark = false))
        assertTrue("深色：深底浅字", KeyboardThemes.readableEnough(darkBg, darkText, dark = true))
        assertTrue("纯黑深色", KeyboardThemes.readableEnough(pureBlackBg, darkText, dark = true))
    }

    @Test
    fun `浅底白字在深色模式下必须被拒绝`() {
        // 就是用户看到的那个组合：底是浅色，字是白色
        assertFalse(KeyboardThemes.readableEnough(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), dark = true))
        assertFalse(KeyboardThemes.readableEnough(lightBg, 0xFFF7F7FA.toInt(), dark = true))
        // 反方向也不行：深色模式下拿了浅色调色板的底色
        assertFalse(KeyboardThemes.readableEnough(lightBg, lightText, dark = true))
    }

    @Test
    fun `浅色模式下拿了深色调色板也要被拒绝`() {
        assertFalse(KeyboardThemes.readableEnough(darkBg, darkText, dark = false))
        assertFalse(KeyboardThemes.readableEnough(pureBlackBg, darkText, dark = false))
    }

    @Test
    fun `对比度不足时即使底色方向对也要被拒绝`() {
        // 深色模式下深灰底 + 中灰字：方向对，但字看不清
        assertFalse(KeyboardThemes.readableEnough(0xFF202124.toInt(), 0xFF2A2C30.toInt(), dark = true))
        // 浅色模式下白底 + 浅灰字
        assertFalse(KeyboardThemes.readableEnough(0xFFFFFFFF.toInt(), 0xFFE8E8E8.toInt(), dark = false))
    }

    @Test
    fun `对比度与亮度的基本性质`() {
        assertTrue(KeyboardThemes.relativeLuminance(0xFFFFFFFF.toInt()) > 0.95)
        assertTrue(KeyboardThemes.relativeLuminance(0xFF000000.toInt()) < 0.05)
        // 黑白对比 = 21:1，同一个颜色跟自己是 1:1
        assertTrue(KeyboardThemes.contrastRatio(0xFFFFFFFF.toInt(), 0xFF000000.toInt()) > 20.9)
        assertTrue(KeyboardThemes.contrastRatio(darkBg, darkBg) < 1.001)
        // 交换顺序结果不变
        val a = KeyboardThemes.contrastRatio(darkBg, darkText)
        val b = KeyboardThemes.contrastRatio(darkText, darkBg)
        assertTrue(kotlin.math.abs(a - b) < 0.0001)
    }

    @Test
    fun `阈值就是 WCAG AA 的小字标准`() {
        assertTrue(KeyboardThemes.MIN_CONTRAST in 4.4..4.6)
    }
}
