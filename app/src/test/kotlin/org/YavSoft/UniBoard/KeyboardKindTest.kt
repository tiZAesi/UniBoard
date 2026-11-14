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
import org.YavSoft.UniBoard.core.KeyboardGroup
import org.YavSoft.UniBoard.core.KeyboardKind

/**
 * 键盘种类的唯一清单：id 不能重复、分组要覆盖全部、旧 id 要能迁移。
 *
 * 这三条正是「切换键盘有时没反应」的根因所在：清单散在多处时，
 * 某个 id 只存在于其中一份，就会出现选了没效果。
 */
class KeyboardKindTest {

    @Test
    fun `id 唯一且连续可用`() {
        val ids = KeyboardKind.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(KeyboardKind.ALL.size, KeyboardKind.chinese.size + KeyboardKind.others.size)
    }

    @Test
    fun `旧版本的笔画键盘 id 迁移成全键盘`() {
        // 旧版本 id=4 是笔画键盘，已移除；老用户读到的 4 不能变成幽灵项
        assertEquals(KeyboardKind.FULL, KeyboardKind.of(KeyboardKind.LEGACY_STROKE_ID))
        assertEquals(KeyboardKind.FULL, KeyboardKind.of(-1))
        assertEquals(KeyboardKind.FULL, KeyboardKind.of(999))
    }

    @Test
    fun `按 id 与按名字都能取到`() {
        assertEquals(KeyboardKind.KANA, KeyboardKind.of(KeyboardKind.KANA.id))
        assertEquals(KeyboardKind.ZHUYIN, KeyboardKind.ofName("ZHUYIN"))
        assertEquals(KeyboardKind.FULL, KeyboardKind.ofName("没有这个键盘"))
    }

    @Test
    fun `日文键盘标记正确`() {
        assertTrue(KeyboardKind.KANA.japanese)
        assertTrue(KeyboardKind.KANA50.japanese)
        assertTrue(!KeyboardKind.FULL.japanese)
        assertTrue(KeyboardKind.chinese.none { it.japanese })
    }
}
