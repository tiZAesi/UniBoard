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

package org.YavSoft.UniBoard.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.YavSoft.UniBoard.core.Prefs

/**
 * 把「主题模式」这一个设置同时落实到三处：
 *
 * 1. **设置页 / 主界面**（AppCompat 的日夜模式）
 * 2. **键盘自身配色**（[KeyboardThemes] 的缓存）
 * 3. **输入法窗口**：输入法不是 Activity，拿不到 AppCompat 的日夜切换，
 *    只能靠 `Configuration.uiMode` + [Prefs.isDarkUi] 自己判断，
 *    所以这里必须清一次 [KeyboardThemes] 的缓存，否则键盘会停在旧配色上。
 *
 * 之前只有第 2 步（设置页改主题色时会 invalidate），第 1 步完全没人做：
 * 于是「主题模式」设成深色时，键盘变深了，设置页却还是白的；
 * 反过来系统深浅色切换时，输入法窗口不会重建，键盘可能停在浅色上。
 */
object AppTheme {

    /** 按设置把日夜模式应用到整个应用；重复调用无副作用 */
    fun apply(context: Context) {
        val prefs = Prefs(context)
        val mode = when (prefs.themeMode) {
            Prefs.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Prefs.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        KeyboardThemes.invalidate()
    }
}
