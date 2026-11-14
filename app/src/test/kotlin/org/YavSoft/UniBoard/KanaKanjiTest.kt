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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.YavSoft.UniBoard.predict.KanaKanji

/**
 * 假名 → 汉字转换测试。
 *
 * 用一份手写的小词表跑，不依赖 assets：这样测试跑得快，也能刻意构造
 * 「词表里没有的助词」这类边界情况。
 */
class KanaKanjiTest {

    private val kk = KanaKanji()

    @Before
    fun setUp() {
        val dict = """
            にほん	日本:100,二本:5000
            がっこう	学校:100
            きょう	今日:100,京:900
            とうきょう	東京:100
            ありがとう	有難う:400,有り難う:500
        """.trimIndent()
        kk.load(dict.reader().buffered())
    }

    @Test
    fun `词表加载与查询`() {
        assertTrue(kk.isLoaded)
        assertEquals("日本", kk.lookup("にほん").first().surface)
    }

    @Test
    fun `整词转换`() {
        assertEquals("日本", kk.convert("にほん").first().text)
        assertEquals("学校", kk.convert("がっこう").first().text)
        assertEquals("東京", kk.convert("とうきょう").first().text)
    }

    @Test
    fun `词表里没有的假名原样保留`() {
        // にほん + ご（词表里没有 ご）→ 日本ご，不能把 ご 吃掉
        assertEquals("日本ご", kk.convert("にほんご").first().text)
        assertEquals("今日は", kk.convert("きょうは").first().text)
    }

    @Test
    fun `其它写法与片假名也在候选里`() {
        val texts = kk.convert("ありがとう", limit = 8).map { it.text }
        assertTrue("应该有第二种写法：$texts", texts.contains("有り難う"))
        // 片假名兜底永远存在，外来语靠它上屏
        assertTrue("应该有片假名：$texts", texts.contains("アリガトウ"))
    }

    @Test
    fun `候选数量受 limit 限制`() {
        val list = kk.convert("ありがとう", limit = 2)
        assertEquals(2, list.size)
    }

    @Test
    fun `空词表也不会崩`() {
        val empty = KanaKanji()
        assertFalse(empty.isLoaded)
        val list = empty.convert("にほん")
        assertEquals("にほん", list.first().text)
        assertTrue(list.any { it.text == "ニホン" })
    }
}
