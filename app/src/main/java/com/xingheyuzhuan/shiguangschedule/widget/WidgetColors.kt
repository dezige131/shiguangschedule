package com.xingheyuzhuan.shiguangschedule.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    // 定义深色模式的辅助颜色
    private val DarkGray1 = Color(0xFF1E1E1E) // 深色背景
    private val DarkWhite = Color(0xFFFFFFFF) // 白色文字
    private val DarkLightGray = Color(0xFFAAAAAA) // 浅灰色次要文字
    private val DarkDivider = Color(0xFF444444) // 深色分割线

    // --- 适配主题的颜色 ---

    // 背景色
    val background = ColorProvider(day = Color(0xFFFFFFFF), night = DarkGray1)

    // 文字颜色
    val textPrimary = ColorProvider(day = Color(0xFF2C3E50), night = DarkWhite)
    val textSecondary = ColorProvider(day = Color(0xFF7F8C8D), night = DarkLightGray)
    val textHint = ColorProvider(day = Color(0xFFBDC3C7), night = Color(0xFF888888))
    val divider = ColorProvider(day = Color(0xFFECF0F1), night = DarkDivider)


    private val PrimaryColor = Color(0xFF3498DB) // 蓝色

    // 主色调 - 顶部日历栏
    val primary = ColorProvider(day = PrimaryColor, night = PrimaryColor)

}
