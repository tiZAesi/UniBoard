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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.YavSoft.UniBoard.predict.RomajiKana

/** 罗马字 → 假名：促音、拨音、拗音、未打完的尾巴 */
class RomajiKanaTest {

    @Test
    fun `基本音与拗音`() {
        assertEquals("かんじ", RomajiKana.convert("kanji").kana)
        assertEquals("とうきょう", RomajiKana.convert("toukyou").kana)
        assertEquals("しゃしん", RomajiKana.convert("shashin").kana)
        assertEquals("きょう", RomajiKana.convert("kyou").kana)
    }

    @Test
    fun `促音用叠辅音`() {
        assertEquals("がっこう", RomajiKana.convert("gakkou").kana)
        assertEquals("まっちゃ", RomajiKana.convert("matcha").kana)
        assertEquals("きって", RomajiKana.convert("kitte").kana)
    }

    @Test
    fun `拨音的边界`() {
        assertEquals("かんたん", RomajiKana.convert("kantan").kana)
        // n 后面接元音时要当一个音节的一部分，不是拨音
        assertEquals("にほんご", RomajiKana.convert("nihongo").kana)
        assertEquals("こんにちは", RomajiKana.convert("konnnichiha").kana)
        // n + y 是 にゃ 行
        assertEquals("にゃ", RomajiKana.convert("nya").kana)
    }

    @Test
    fun `没打完的半截罗马字留在 pending 里`() {
        val r = RomajiKana.convert("ky")
        assertEquals("", r.kana)
        assertEquals("ky", r.pending)
        assertTrue(r.isPending)

        val r2 = RomajiKana.convert("kangae")
        assertEquals("かんがえ", r2.kana)
        assertFalse(r2.isPending)
    }

    @Test
    fun `片假名与小假名`() {
        assertEquals("カタカナ", RomajiKana.toKatakana("かたかな"))
        assertEquals("ぁ", RomajiKana.convert("xa").kana)
        assertEquals("っ", RomajiKana.convert("xtu").kana)
        assertEquals("ー", RomajiKana.convert("-").kana)
    }

    @Test
    fun `平假名与片假名互转`() {
        assertEquals("ひらがな", RomajiKana.toHiraganaFromKatakana("ヒラガナ"))
    }

    @Test
    fun `浊点合并`() {
        assertEquals("が", RomajiKana.voice("か"))
        assertEquals("ぱ", RomajiKana.voice("は", handakuten = true))
        // 已经是浊音时再点一次退回清音，方便改错
        assertEquals("か", RomajiKana.voice("が"))
        assertEquals("ガ", RomajiKana.voice("カ"))
        assertNull(RomajiKana.voice("あ"))
        assertNull(RomajiKana.voice(""))
    }
}
