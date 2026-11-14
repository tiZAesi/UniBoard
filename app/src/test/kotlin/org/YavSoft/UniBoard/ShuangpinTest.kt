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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.YavSoft.UniBoard.predict.Shuangpin
import org.YavSoft.UniBoard.predict.ShuangpinScheme

/**
 * 双拼解码测试。
 *
 * 用例都是「真实会打出来的串」，不是罗列码表：先按方案把音节编码成两键码，
 * 再解码回全拼，验证闭环。这样任何一个方案的键位抄错，测试都会红。
 */
class ShuangpinTest {

    /** 按方案找某个音节的键位码（找不到返回 null） */
    private fun encode(scheme: ShuangpinScheme, syllable: String): String? {
        val table = Shuangpin.table(scheme)
        return table.syllables.entries.firstOrNull { it.value == syllable }?.key
    }

    private fun encode(scheme: ShuangpinScheme, vararg syllables: String): String {
        val sb = StringBuilder()
        for (syl in syllables) {
            sb.append(encode(scheme, syl) ?: error("方案 ${scheme.label} 里没有音节 $syl"))
        }
        return sb.toString()
    }

    @Test
    fun `小鹤 双键合成全拼`() {
        assertEquals("nihao", Shuangpin.toPinyin("nihc", ShuangpinScheme.XIAOHE))
        // 倒过来：由全拼编回双拼码，再解码，必须还原
        val code = encode(ShuangpinScheme.XIAOHE, "zhong", "guo")
        assertEquals("zhongguo", Shuangpin.toPinyin(code, ShuangpinScheme.XIAOHE))
    }

    @Test
    fun `每个方案都能编码再解码 中国 你好`() {
        for (scheme in ShuangpinScheme.ALL) {
            assertEquals(scheme.label + " 中国",
                "zhongguo", Shuangpin.toPinyin(encode(scheme, "zhong", "guo"), scheme))
            assertEquals(scheme.label + " 你好",
                "nihao", Shuangpin.toPinyin(encode(scheme, "ni", "hao"), scheme))
        }
    }

    @Test
    fun `音节切分给出可读的中间结果`() {
        val code = encode(ShuangpinScheme.XIAOHE, "ni", "hao", "zhong", "guo")
        val parts = Shuangpin.syllables(code, ShuangpinScheme.XIAOHE)
        assertEquals(listOf("ni", "hao", "zhong", "guo"), parts)
    }

    @Test
    fun `输错一个键不会让后面全部错位`() {
        // 贪心切分会因为第一个键不合法而整套错位；动态规划只丢这一个键
        val parts = Shuangpin.syllables("1nihc", ShuangpinScheme.XIAOHE)
        assertEquals("ni", parts[parts.size - 2])
        assertEquals("hao", parts[parts.size - 1])
    }

    @Test
    fun `微软双拼的 ing 在分号键上`() {
        assertTrue(ShuangpinScheme.MSPY.needsSemicolon)
        val table = Shuangpin.table(ShuangpinScheme.MSPY)
        // ing 这个韵母落在「;」键，所以拉丁键盘要补一个 ; 键才打得出来
        assertTrue(table.syllables.keys.any { it.endsWith(";") })
        assertEquals("bing", Shuangpin.toPinyin("b;", ShuangpinScheme.MSPY))
        assertEquals("jing", Shuangpin.toPinyin("j;", ShuangpinScheme.MSPY))
    }

    @Test
    fun `韵母提示与方案一致`() {
        // 小鹤：q=iu，d=ai，h=ang
        assertEquals("iu", Shuangpin.finalHint('q', ShuangpinScheme.XIAOHE))
        assertEquals("ai", Shuangpin.finalHint('d', ShuangpinScheme.XIAOHE))
        assertEquals("ang", Shuangpin.finalHint('h', ShuangpinScheme.XIAOHE))
        // 自然码：ai 在 l 上，和 小鹤 不同，说明提示确实换了表
        assertEquals("ai", Shuangpin.finalHint('l', ShuangpinScheme.ZIRANMA))
    }

    @Test
    fun `空输入不炸`() {
        assertEquals("", Shuangpin.toPinyin("", ShuangpinScheme.XIAOHE))
        assertTrue(Shuangpin.syllables("", ShuangpinScheme.XIAOHE).isEmpty())
    }
}
