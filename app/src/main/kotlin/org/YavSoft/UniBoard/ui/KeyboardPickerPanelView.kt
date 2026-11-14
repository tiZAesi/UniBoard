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
import org.YavSoft.UniBoard.core.KeyboardGroup
import org.YavSoft.UniBoard.core.KeyboardKind
import org.YavSoft.UniBoard.core.Prefs
import org.YavSoft.UniBoard.core.dpInt
import org.YavSoft.UniBoard.ui.theme.KeyboardTheme
import org.YavSoft.UniBoard.ui.theme.KeyboardThemes

/**
 * 键盘选择面板。
 *
 * 只列键盘本身，不再写一长串说明：按「中文输入 / 其他语言」分组，
 * 每行一个键盘名，当前项右侧打勾。面板高度按内容自适应，
 * 但最多显示到键盘高度的量级，避免遮住整个屏幕。
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

    /** 分组顺序与内容都来自 [KeyboardKind]，不再在这里维护第二份键盘清单 */
    private val groups: List<Pair<KeyboardGroup, List<KeyboardKind>>> = listOf(
        KeyboardGroup.CHINESE to KeyboardKind.chinese,
        KeyboardGroup.OTHER to KeyboardKind.others
    )

    /** 双拼那一行要带上当前方案名，否则五个方案长得一样 */
    private fun nameOf(kind: KeyboardKind): String = when (kind) {
        KeyboardKind.SHUANGPIN ->
            "${kind.label}（${org.YavSoft.UniBoard.predict.ShuangpinScheme.of(prefs.shuangpinScheme).short}）"
        else -> kind.label
    }

    init {
        orientation = VERTICAL
        setPadding(0, context.dpInt(4f), 0, context.dpInt(6f))
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
        for ((group, kinds) in groups) {
            addView(groupLabel(group.label))
            for (kind in kinds) addView(optionRow(kind))
        }
    }

    private fun titleRow(): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpInt(16f), context.dpInt(2f), context.dpInt(12f), context.dpInt(2f))
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

    private fun groupLabel(text: String): View = TextView(context).apply {
        this.text = text
        textSize = 11f
        setTextColor(theme.accent)
        setPadding(context.dpInt(16f), context.dpInt(8f), context.dpInt(16f), context.dpInt(2f))
    }

    private fun optionRow(kind: KeyboardKind): View {
        val selected = prefs.keyboardKind == kind
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpInt(24f), context.dpInt(7f), context.dpInt(16f), context.dpInt(7f))
            isClickable = true
            setOnClickListener { listener?.onPick(kind.id) }
        }
        val text = LinearLayout(context).apply { orientation = VERTICAL }
        text.addView(TextView(context).apply {
            this.text = nameOf(kind)
            textSize = 15f
            setTextColor(if (selected) theme.accent else theme.textPrimary)
            setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        })
        text.addView(TextView(context).apply {
            this.text = kind.description
            textSize = 11f
            setTextColor(theme.textSecondary)
        })
        row.addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (selected) {
            row.addView(TextView(context).apply {
                this.text = "✓"
                textSize = 15f
                setTextColor(theme.accent)
            })
        }
        return row
    }

    fun refresh() = rebuild()
}
