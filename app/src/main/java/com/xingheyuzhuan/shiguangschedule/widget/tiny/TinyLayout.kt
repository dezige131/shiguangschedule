package com.xingheyuzhuan.shiguangschedule.widget.tiny

import android.graphics.Bitmap
import android.graphics.Paint
import android.text.Layout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.core.GCanvasViewport
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.dsl.gcText
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TinyLayout(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val safePaddingValue = 42f
    val density = context.resources.displayMetrics.density

    // 1. 业务逻辑
    val allCourses = snapshot.coursesList
    val currentWeek = if (snapshot.currentWeek == 0) null else snapshot.currentWeek
    val styleProto = snapshot.style
    val now = LocalTime.now()
    val todayStr = LocalDate.now().toString()

    val isVacation = currentWeek == null
    val todayCourses = allCourses.filter { it.date == todayStr || it.date.isBlank() }
    val nextCourse = todayCourses.firstOrNull { !it.isSkipped && LocalTime.parse(it.endTime) > now }

    val maskPrimary = android.graphics.Color.BLACK
    val maskSecondary = android.graphics.Color.argb(165, 0, 0, 0)
    val maskHint = android.graphics.Color.argb(130, 0, 0, 0)

    GCanvasViewport(
        designWidth = 412f,
        designHeight = 210f,
        snapshot = snapshot,
        cornerRadius = 55f,
        safePadding = safePaddingValue,
        backgroundColor = WidgetColors.background,
        tintColor = WidgetColors.textPrimary,
        content = { metrics ->
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                if (!isVacation && nextCourse != null) {
                    val nextCourseIndex = todayCourses.indexOf(nextCourse)
                    val remainingCount = todayCourses.size - nextCourseIndex
                    val remainingText = remainingCount.toString()

                    val bubbleDesignSize = 60f
                    val scaleFactor = metrics.physicalWidth / metrics.designWidth
                    val displaySizeDp = (bubbleDesignSize * scaleFactor / density).dp
                    val safePaddingDp = (safePaddingValue * scaleFactor / density).dp

                    // 气泡颜色逻辑
                    val colorPair = if (nextCourse.colorInt in 0 until styleProto.courseColorMapsCount) {
                        styleProto.getCourseColorMaps(nextCourse.colorInt)
                    } else null
                    val bubbleColorProvider = if (colorPair != null) {
                        ColorProvider(
                            day = Color(colorPair.lightColor),
                            night = Color(if (colorPair.darkColor != 0L) colorPair.darkColor else colorPair.lightColor)
                        )
                    } else { GlanceTheme.colors.secondaryContainer }

                    Box(
                        modifier = GlanceModifier.fillMaxSize().padding(top = safePaddingDp, end = safePaddingDp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(displaySizeDp)
                                .height(displaySizeDp)
                                .background(bubbleColorProvider)
                                .cornerRadius(displaySizeDp / 2f),
                            contentAlignment = Alignment.Center
                        ) {
                            val textBitmap = remember(remainingText, metrics.finalScale) {
                                val pxSize = (bubbleDesignSize * metrics.finalScale).toInt().coerceAtLeast(1)
                                createBitmap(pxSize, pxSize, Bitmap.Config.ARGB_8888).applyCanvas {
                                    scale(metrics.finalScale, metrics.finalScale)
                                    val paint = Paint().apply {
                                        isAntiAlias = true
                                        color = android.graphics.Color.WHITE
                                        textSize = 38f
                                        textAlign = Paint.Align.CENTER
                                        isFakeBoldText = true
                                    }
                                    drawText(remainingText, bubbleDesignSize / 2f, bubbleDesignSize * 0.7f, paint)
                                }
                            }
                            Image(
                                provider = ImageProvider(textBitmap),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        },
        onDraw = { canvas, _ ->
            val contentWidth = 412f - 84f
            val centerY = (210f - 84f) / 2f

            when {
                // 1. 假期状态
                isVacation -> {
                    canvas.gcText(context.getString(R.string.title_vacation), 0f, centerY - 35f, 40f, maskPrimary, isBold = true, maxWidth = contentWidth, alignment = Layout.Alignment.ALIGN_CENTER)
                    canvas.gcText(context.getString(R.string.widget_vacation_expecting), 0f, centerY + 15f, 30f, maskSecondary, maxWidth = contentWidth, alignment = Layout.Alignment.ALIGN_CENTER)
                }

                // 2. 有课状态
                nextCourse != null -> {
                    val textMaxWidth = 260f
                    canvas.gcText(nextCourse.name, 0f, 0f, 40f, maskPrimary, isBold = true, maxWidth = textMaxWidth, maxLines = 1)
                    canvas.gcText("${nextCourse.startTime.take(5)} - ${nextCourse.endTime.take(5)}", 0f, 40f, 35f, maskSecondary)
                    if (nextCourse.position.isNotBlank()) {
                        canvas.gcText(nextCourse.position, 0f, 75f, 35f, maskHint, maxWidth = textMaxWidth, maxLines = 1)
                    }
                }

                // 3. 今日课程结束或无课
                else -> {
                    val tip = if (allCourses.isEmpty()) context.getString(R.string.text_no_courses_today)
                    else context.getString(R.string.widget_today_courses_finished)
                    canvas.gcText(tip, 0f, centerY - 20f, 35f, maskSecondary, maxWidth = contentWidth, alignment = Layout.Alignment.ALIGN_CENTER)
                }
            }
        }
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 412, heightDp = 210)
@Composable
fun TinyLayoutGlancePreview() {
    val todayStr = LocalDate.now().toString()
    val mockStyle = com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto.newBuilder().apply {
        addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder()
            .setLightColor(0xFF3498DB).build())
        addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder()
            .setLightColor(0xFFE67E22).build())
    }.build()

    val mockSnapshot = WidgetSnapshot.newBuilder().apply {
        currentWeek = 12
        style = mockStyle
        addCourses(WidgetCourseProto.newBuilder()
            .setName("计算机网络")
            .setStartTime("08:30")
            .setEndTime("23:59")
            .setPosition("实验楼 402")
            .setColorInt(0)
            .setDate(todayStr)
            .build())

    }.build()

    TinyLayout(snapshot = mockSnapshot)
}