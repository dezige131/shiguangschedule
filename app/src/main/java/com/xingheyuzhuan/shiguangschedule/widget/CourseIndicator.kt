package com.xingheyuzhuan.shiguangschedule.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.core.GCanvasMetrics

/**
 * 专用于双日或列表布局的课程侧边指示条
 * 采用原生 Glance 组件渲染以适配系统深色模式切换。
 *
 * @param x 设计稿上的 X 坐标 (不含 safePadding)
 * @param y 设计稿上的 Y 坐标 (不含 safePadding)
 * @param height 设计稿上的高度
 * @param colorInt 颜色索引 (对应 Proto 中的样式)
 * @param metrics GCanvasViewport 提供的度量衡 (用于坐标转换)
 * @param styleProto 包含颜色映射的样式数据
 */
@Composable
fun CourseIndicator(
    x: Float,
    y: Float,
    height: Float,
    colorInt: Int,
    metrics: GCanvasMetrics,
    styleProto: ScheduleGridStyleProto,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    // 1. 颜色映射逻辑
    val colorPair = if (colorInt in 0 until styleProto.courseColorMapsCount) {
        styleProto.getCourseColorMaps(colorInt)
    } else null

    val colorProvider = if (colorPair != null) {
        ColorProvider(
            day = Color(colorPair.lightColor.toLong()),
            night = Color(if (colorPair.darkColor != 0L) colorPair.darkColor.toLong() else colorPair.lightColor.toLong())
        )
    } else {
        GlanceTheme.colors.secondaryContainer // 默认兜底色
    }

    // 2. 坐标与尺寸映射
    // 使用引擎的 finalScale 将设计稿 px 转换为物理 px，再除以 density 转换为设备 dp
    val scale = metrics.finalScale
    val leftDp = ((x + metrics.safePadding) * scale / density).dp
    val topDp = ((y + metrics.safePadding) * scale / density).dp

    // 指示条宽度固定为 8px (设计稿基准)
    val widthDp = (8f * scale / density).dp
    val heightDp = (height * scale / density).dp

    Box(
        modifier = modifier
            .padding(start = leftDp, top = topDp)
            .width(widthDp)
            .height(heightDp)
            .background(colorProvider)
            .cornerRadius(4f.dp) // 固定小圆角
    ) {}
}