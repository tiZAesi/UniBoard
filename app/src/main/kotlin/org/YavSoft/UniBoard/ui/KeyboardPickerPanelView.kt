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

package org.YavSoft.UniBoard.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.YavSoft.UniBoard.core.Prefs
import org.YavSoft.UniBoard.core.dpInt
import org.YavSoft.UniBoard.ui.theme.KeyboardTheme
import org.YavSoft.UniBoard.ui.theme.KeyboardThemes

/**
 * 键盘选择面板。
 *
 * 点候选栏的「切换键盘」或九键右下角的键盘键时弹出，
 * 一屏列出现有键盘（全键盘 / 九键 / 明快），点哪项就切到哪项，
 * 比原来的「循环切换」更容易看见当前状态，也不会点两下才转一圈。
 */
class KeyboardPickerPanelView(context: Context) : LinearLayout(context) {

    interface Listener {
        fun onPick(type: Int)
        fun onClose()
    }

    var listener: Listener? = null

    var theme: KeyboardTheme = KeyboardThemes.resolve(context, Prefs(context))
        set(value) {
            field = value
            rebuild()
        }

    private val prefs = Prefs(context)

    private data class Option(val type: Int, val name: String, val desc: String)

    private val options = listOf(
        Option(Prefs.KB_FULL, "全键盘", "QWERTY 全键盘，拼音与英文都能打"),
        Option(Prefs.KB_NINE, "九键（T9）", "2–9 数字键出拼音候选，单手更省力"),
        Option(Prefs.KB_MINGKWAI, "明快键盘", "上形 + 下形两键取字，参考明快打字机"),
        Option(Prefs.KB_SHUANGPIN, "双拼（小鹤）", "每个音节两键：声母键 + 韵母键"),
        Option(Prefs.KB_STROKE, "笔画输入", "横竖撇捺折，逐笔缩小范围，不认字也能打"),
        Option(Prefs.KB_T9_EN, "英文九键", "数字键打英文，T9 前缀匹配"),
        Option(Prefs.KB_KANA, "日文假名", "五十音两页，点一下就读入假名")
    )

    init {
        orientation = VERTICAL
        setPadding(0, context.dpInt(6f), 0, context.dpInt(8f))
        rebuild()
    }

    private fun rebuild() {
        removeAllViews()
        background = GradientDrawable().apply {
            cornerRadii = floatArrayOf(
                context.dpInt(18f).toFloat(), context.dpInt(18f).toFloat(),
                context.dpInt(18f).toFloat(), context.dpInt(18f).toFloat(),
                0f, 0f, 0f, 0f
            )
            setColor(theme.keyboardBackground)
        }

        addView(titleRow())
        for (option in options) {
            addView(optionRow(option))
        }
    }

    private fun titleRow(): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpInt(16f), context.dpInt(4f), context.dpInt(12f), context.dpInt(4f))
        }
        row.addView(TextView(context).apply {
            text = "选择键盘"
            textSize = 13f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(theme.textSecondary)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(context).apply {
            text = "关闭"
            textSize = 13f
            setTextColor(theme.accent)
            setPadding(context.dpInt(10f), context.dpInt(4f), context.dpInt(4f), context.dpInt(4f))
            setOnClickListener { listener?.onClose() }
        })
        return row
    }

    private fun optionRow(option: Option): View {
        val selected = prefs.keyboardType == option.type
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpInt(16f), context.dpInt(12f), context.dpInt(16f), context.dpInt(12f))
            isClickable = true
            setOnClickListener { listener?.onPick(option.type) }
        }
        val texts = LinearLayout(context).apply { orientation = VERTICAL }
        texts.addView(TextView(context).apply {
            text = option.name
            textSize = 16f
            setTextColor(if (selected) theme.accent else theme.textPrimary)
            setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        })
        texts.addView(TextView(context).apply {
            text = option.desc
            textSize = 11.5f
            setTextColor(theme.hintText)
            setPadding(0, context.dpInt(2f), 0, 0)
        })
        row.addView(texts, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (selected) {
            row.addView(TextView(context).apply {
                text = "✓"
                textSize = 16f
                setTextColor(theme.accent)
            })
        }
        return row
    }

    /** 选项被外部改动后刷新高亮 */
    fun refresh() = rebuild()
}
