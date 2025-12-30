package com.xingheyuzhuan.shiguangschedule.widget.gcanvas.dsl

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.LocalContext
import androidx.glance.GlanceModifier
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.core.GCanvasMetrics

/**
 * 基础矩形绘制 (支持圆角)
 */
fun Canvas.gcBox(
    l: Float, t: Float, r: Float, b: Float,
    color: Int,
    radius: Float = 0f
) {
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        style = Paint.Style.FILL
    }

    if (radius > 0f) {
        this.drawRoundRect(RectF(l, t, r, b), radius, radius, paint)
    } else {
        this.drawRect(l, t, r, b, paint)
    }
}

/**
 * 文字绘制
 * 1. 支持 \n 换行符。
 * 2. 支持 maxWidth 自动折行。
 * 3. 支持 maxLines 配合省略号 (Ellipsize)。
 * 4. 新增 alignment 参数支持对齐。
 */
fun Canvas.gcText(
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Int,
    isBold: Boolean = false,
    maxWidth: Float = 1000f,
    maxLines: Int = Int.MAX_VALUE,
    lineSpacingMultiplier: Float = 1.0f,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
) {
    // 1. 构造专用的 TextPaint
    val textPaint = TextPaint().apply {
        isAntiAlias = true
        this.color = color
        this.textSize = size
        typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    // 2. 使用 StaticLayout.Builder 进行排版
    val staticLayout = StaticLayout.Builder
        .obtain(text, 0, text.length, textPaint, maxWidth.toInt().coerceAtLeast(1))
        // --- 关键修改点：应用传入的对齐方式 ---
        .setAlignment(alignment)
        .setLineSpacing(0f, lineSpacingMultiplier)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setEllipsizedWidth(maxWidth.toInt().coerceAtLeast(1))
        .build()

    // 3. 执行绘制
    this.save()
    this.translate(x, y)
    staticLayout.draw(this)
    this.restore()
}

/**
 * 分割线/线条绘制
 */
fun Canvas.gcLine(
    startX: Float, startY: Float,
    endX: Float, endY: Float,
    width: Float,
    color: Int
) {
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        strokeWidth = width
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND // 线条末端圆角处理，视觉更精致
    }
    this.drawLine(startX, startY, endX, endY, paint)
}

/**
 * Compose 交互层定位容器
 * 将 Canvas 设计稿坐标自动转换为 Glance Dp 坐标
 */
@Composable
fun GCanvasMetrics.GCBox(
    x: Float,          // 设计稿 X (不含 safePadding)
    y: Float,          // 设计稿 Y (不含 safePadding)
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    // 计算缩放比：引擎内部的缩放系数 / 系统密度
    val factor = finalScale / density

    // 核心转换公式：(坐标 + 容器内边距) * 缩放比
    val startDp = ((x + safePadding) * factor).dp
    val topDp = ((y + safePadding) * factor).dp

    Box(
        modifier = GlanceModifier
            .padding(start = startDp, top = topDp)
            .then(modifier),
        content = { content() }
    )
}