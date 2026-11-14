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
 * 按「中文输入 / 其它语言」分组，每行一个键盘名 + 一行说明，当前项右侧打勾。
 *
 * 双拼那一行支持**长按切换方案**：长按后本面板原地换成五个方案的选择列表
 * （小鹤 / 自然码 / 微软 / 智能 ABC / 拼音加加），点一下即可换，标题栏有「返回」。
 * 不另起弹窗，是因为输入法窗口里弹 Dialog 容易和键盘抢焦点、在部分 ROM 上还会露出白底。
 */
class KeyboardPickerPanelView(context: Context) : LinearLayout(context) {

    interface Listener {
        /** 选中某个键盘 */
        fun onPick(type: Int)

        /** 选中某个双拼方案（[org.YavSoft.UniBoard.predict.ShuangpinScheme.id]） */
        fun onPickScheme(schemeId: Int) {}

        fun onClose()
    }

    var listener: Listener? = null

    var theme: KeyboardTheme = KeyboardThemes.resolve(context, Prefs(context))
        set(value) {
            field = value
            rebuild()
        }

    private val prefs = Prefs(context)

    /** 是否正在显示「双拼方案」子列表 */
    private var showingSchemes = false

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
        if (showingSchemes) {
            addView(groupLabel("双拼方案（点一下即生效）"))
            for (scheme in org.YavSoft.UniBoard.predict.ShuangpinScheme.ALL) {
                addView(schemeRow(scheme))
            }
            return
        }
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
        if (showingSchemes) {
            row.addView(TextView(context).apply {
                text = "‹ 返回"
                textSize = 13f
                setTextColor(theme.accent)
                setPadding(0, context.dpInt(4f), context.dpInt(10f), context.dpInt(4f))
                isClickable = true
                setOnClickListener {
                    showingSchemes = false
                    rebuild()
                    requestLayout()
                }
            })
        }
        row.addView(TextView(context).apply {
            text = if (showingSchemes) "选择双拼方案" else "选择键盘"
            textSize = 13f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(theme.textSecondary)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(context).apply {
            text = "关闭"
            textSize = 13f
            setTextColor(theme.accent)
            setPadding(context.dpInt(10f), context.dpInt(4f), context.dpInt(4f), context.dpInt(4f))
            isClickable = true
            setOnClickListener { listener?.onClose() }
        })
        return row
    }

    /** 双拼方案行：当前方案打勾 */
    private fun schemeRow(scheme: org.YavSoft.UniBoard.predict.ShuangpinScheme): View {
        val selected = prefs.shuangpinScheme == scheme.id
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpInt(24f), context.dpInt(9f), context.dpInt(16f), context.dpInt(9f))
            isClickable = true
            setOnClickListener { listener?.onPickScheme(scheme.id) }
        }
        row.addView(TextView(context).apply {
            text = scheme.label
            textSize = 15f
            setTextColor(if (selected) theme.accent else theme.textPrimary)
            setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (selected) {
            row.addView(TextView(context).apply {
                text = "✓"
                textSize = 15f
                setTextColor(theme.accent)
            })
        }
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
            if (kind == KeyboardKind.SHUANGPIN) {
                // 长按进方案列表：5 个方案挤不进面板主列表，但切换又必须够快
                isLongClickable = true
                setOnLongClickListener {
                    showingSchemes = true
                    rebuild()
                    requestLayout()
                    true
                }
            }
        }
        val text = LinearLayout(context).apply { orientation = VERTICAL }
        text.addView(TextView(context).apply {
            this.text = nameOf(kind)
            textSize = 15f
            setTextColor(if (selected) theme.accent else theme.textPrimary)
            setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        })
        text.addView(TextView(context).apply {
            this.text = if (kind == KeyboardKind.SHUANGPIN) {
                kind.description + " · 长按切换方案"
            } else {
                kind.description
            }
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

    /** 外部每次打开面板时调用：回到键盘主列表，不保留上次的方案子列表 */
    fun refresh() {
        showingSchemes = false
        rebuild()
    }
}
