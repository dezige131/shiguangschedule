package com.xingheyuzhuan.shiguangschedule.widget.gcanvas.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider

@Composable
fun GCanvasViewport(
    designWidth: Float,              // 设计稿宽度基准
    designHeight: Float,             // 设计稿高度基准
    snapshot: Any,                   // 快照标识（数据变化驱动重绘）
    backgroundImage: ImageProvider? = null, // 图片背景
    backgroundColor: ColorProvider? = null, // 纯色背景支持
    tintColor: ColorProvider? = null,       // 系统级染色开关
    cornerRadius: Float = 0f,        // 逻辑设计稿圆角
    safePadding: Float = 0f,         // 逻辑设计稿内边距
    clipContent: Boolean = true,     // 是否裁剪内容
    isHighQuality: Boolean = true,   // 抗锯齿
    background: ((Canvas, Float, Float) -> Unit)? = null, // 静态 Canvas 装饰
    content: @Composable (GCanvasMetrics) -> Unit = {},  // 原生交互层 (如课程 Box)
    onDraw: (Canvas, GCanvasMetrics) -> Unit             // 核心蒙版绘制回调
) {
    val size = LocalSize.current
    val context = LocalContext.current
    val systemDensity = context.resources.displayMetrics.density

    val metrics = remember(size) {
        val physicalScreenW = size.width.value * systemDensity
        val physicalScreenH = size.height.value * systemDensity
        val scale = minOf(physicalScreenW / designWidth, physicalScreenH / designHeight)
        val pW = designWidth * scale
        val pH = designHeight * scale


        GCanvasMetrics(
            designWidth = designWidth,
            designHeight = designHeight,
            finalScale = scale,
            safePadding = safePadding,
            contentOffsetX = (physicalScreenW - pW) / 2f,
            contentOffsetY = (physicalScreenH - pH) / 2f,
            physicalWidth = pW,
            physicalHeight = pH
        )
    }

    val bitmap = remember(snapshot, metrics) {
        val pxW = metrics.physicalWidth.toInt().coerceAtLeast(1)
        val pxH = metrics.physicalHeight.toInt().coerceAtLeast(1)

        createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888).applyCanvas {
            if (isHighQuality) {
                drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            }
            if (cornerRadius > 0f) {
                val path = Path().apply {
                    val pxRadius = cornerRadius * metrics.finalScale
                    addRoundRect(RectF(0f, 0f, pxW.toFloat(), pxH.toFloat()), pxRadius, pxRadius, Path.Direction.CW)
                }
                clipPath(path)
            }
            withSave {
                scale(metrics.finalScale, metrics.finalScale)
                background?.invoke(this, designWidth, designHeight)
            }
            withSave {
                scale(metrics.finalScale, metrics.finalScale)
                translate(safePadding, safePadding)
                if (clipContent) clipRect(0f, 0f, designWidth - (safePadding * 2), designHeight - (safePadding * 2))
                onDraw(this, metrics)
            }
        }
    }

    Box(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground(),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = (metrics.physicalWidth / systemDensity).dp
        val heightDp = (metrics.physicalHeight / systemDensity).dp
        val physicalCornerRadiusDp = (cornerRadius * metrics.finalScale / systemDensity).dp

        // 构建受控内容区的 Modifier
        var contentModifier = GlanceModifier
            .width(widthDp)
            .height(heightDp)
            .cornerRadius(physicalCornerRadiusDp)

        // 【核心逻辑】：按优先级叠加背景
        if (backgroundColor != null) {
            contentModifier = contentModifier.background(backgroundColor)
        }
        if (backgroundImage != null) {
            contentModifier = contentModifier.background(backgroundImage)
        }

        Box(
            modifier = contentModifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                colorFilter = tintColor?.let { ColorFilter.tint(it) },
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Box(modifier = GlanceModifier.fillMaxSize()) {
                content(metrics)
            }
        }
    }
}