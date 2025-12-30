package com.xingheyuzhuan.shiguangschedule.widget.compact

import android.text.Layout
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.CourseIndicator
import com.xingheyuzhuan.shiguangschedule.widget.WidgetColors
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.core.GCanvasViewport
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.dsl.GCBox
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.dsl.gcLine
import com.xingheyuzhuan.shiguangschedule.widget.gcanvas.dsl.gcText
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.time.format.TextStyle as LocalDateTextStyle

@Composable
fun CompactLayout(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val allCourses = snapshot.coursesList
    val currentWeek = if (snapshot.currentWeek == 0) null else snapshot.currentWeek
    val styleProto = snapshot.style

    // 1. 数据逻辑
    val now = LocalTime.now()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val todayRemaining = allCourses.filter {
        (it.date == today.toString() || it.date.isBlank()) && !it.isSkipped && LocalTime.parse(it.endTime) > now
    }
    val tomorrowCourses = allCourses.filter { it.date == tomorrow.toString() && !it.isSkipped }
    val isVacation = currentWeek == null
    val isShowingTomorrow = !isVacation && todayRemaining.isEmpty() && tomorrowCourses.isNotEmpty()
    val displayCourses = if (isShowingTomorrow) tomorrowCourses.take(2) else todayRemaining.take(2)
    val totalCount = if (isShowingTomorrow) tomorrowCourses.size else todayRemaining.size
    val displayDate = if (isShowingTomorrow) tomorrow else today

    // 蒙版颜色定义
    val maskPrimary = android.graphics.Color.BLACK  // 课程名：完全不透明
    val maskSecondary = android.graphics.Color.argb(165, 0, 0, 0) // 详情：约 65% 透明度
    val maskHint = android.graphics.Color.argb(140, 0, 0, 0) // 辅助：约 55% 透明度
    val maskDivider = android.graphics.Color.argb(45, 0, 0, 0)  // 分割线：约 18% 透明度

    // --- 布局常量 ---
    val listStartY = 60f
    val itemHeight = 115f
    val textStartX = 20f
    val footerBottomOffset = 30f

    GCanvasViewport(
        designWidth = 412f,
        designHeight = 412f,
        snapshot = snapshot,
        cornerRadius = 55f,
        safePadding = 42f,
        backgroundColor = WidgetColors.background,
        tintColor = WidgetColors.textPrimary,
        content = { metrics ->
            Box(
                modifier = androidx.glance.GlanceModifier
                    .fillMaxSize()
                    .clickable(androidx.glance.action.actionStartActivity<com.xingheyuzhuan.shiguangschedule.MainActivity>())
            ) {
                if (!isVacation && displayCourses.isNotEmpty()) {
                    displayCourses.forEachIndexed { index, _ ->
                        val yItem = listStartY + (index * itemHeight)
                        metrics.GCBox(x = 0f, y = yItem + 10f) {
                            CourseIndicator(0f, 0f, 85f, displayCourses[index].colorInt, metrics, styleProto)
                        }
                    }
                }
            }
        },
        onDraw = { canvas, _ ->
            val contentWidth = 412f - 80f
            val netHeight = 412f - 80f

            val headerTitle = if (isShowingTomorrow) {
                context.getString(R.string.widget_tomorrow_course_preview)
            } else {
                displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())
            }
            canvas.gcText(headerTitle, 0f, 0f, 28f, maskHint)

            if (currentWeek != null) {
                val weekStr = context.getString(R.string.status_current_week_format, currentWeek)
                canvas.gcText(weekStr, 0f, 0f, 28f, maskHint, maxWidth = contentWidth, alignment = Layout.Alignment.ALIGN_OPPOSITE)
            }

            // 核心展示区域
            when {
                isVacation -> drawCenterStatus(
                    canvas, contentWidth, netHeight,
                    context.getString(R.string.title_vacation),
                    context.getString(R.string.widget_vacation_expecting),
                    maskPrimary, maskSecondary
                )

                displayCourses.isEmpty() -> {
                    val statusText = if (isShowingTomorrow) context.getString(R.string.widget_no_courses_tomorrow)
                    else context.getString(R.string.widget_today_courses_finished)
                    drawCenterStatus(canvas, contentWidth, netHeight, statusText, "", maskPrimary, maskSecondary)
                }

                else -> {
                    displayCourses.forEachIndexed { i, course ->
                        val yItem = listStartY + (i * itemHeight)

                        drawCourseItemMask(
                            canvas = canvas,
                            x = textStartX,
                            y = yItem,
                            maxWidth = contentWidth - textStartX,
                            course = course,
                            primary = maskPrimary,
                            secondary = maskSecondary
                        )

                        if (i == 0 && displayCourses.size > 1) {
                            canvas.gcLine(5f, yItem + 110f, contentWidth, yItem + 110f, 1f, maskDivider)
                        }
                    }

                    if (totalCount > 0) {
                        val resId = if (isShowingTomorrow) R.string.widget_remaining_courses_format_tomorrow
                        else R.string.widget_remaining_courses_format_today
                        val footerText = context.getString(resId, totalCount)
                        canvas.gcText(footerText, 0f, netHeight - footerBottomOffset, 24f, maskHint, maxWidth = contentWidth, alignment = Layout.Alignment.ALIGN_CENTER)
                    }
                }
            }
        }
    )
}

/**
 * 绘制课程项：使用形状蒙版逻辑
 */
private fun drawCourseItemMask(
    canvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    maxWidth: Float,
    course: WidgetCourseProto,
    primary: Int,
    secondary: Int
) {
    // 1. 标题行 (加粗蒙版)
    canvas.gcText(course.name, x, y - 6f, 36f, primary, isBold = true, maxWidth = maxWidth, maxLines = 1)

    // 2. 详情行
    val subY = y + 42f
    val timeStr = "${course.startTime.take(5)}-${course.endTime.take(5)}"

    if (course.position.isNotBlank()) {
        canvas.gcText(course.position, x, subY, 28f, secondary, maxWidth = maxWidth - 120f, maxLines = 1)
    }
    canvas.gcText(timeStr, x - 5f, subY, 28f, secondary, maxWidth = maxWidth, alignment = Layout.Alignment.ALIGN_OPPOSITE)

    // 3. 教师行
    if (course.teacher.isNotBlank()) {
        canvas.gcText(course.teacher, x, subY + 34f, 28f, secondary, maxWidth = maxWidth, maxLines = 1)
    }
}

/**
 * 绘制居中状态文字 (假期或空课)
 */
private fun drawCenterStatus(
    canvas: android.graphics.Canvas,
    width: Float,
    height: Float,
    title: String,
    subTitle: String,
    primary: Int,
    secondary: Int
) {
    val centerY = height / 2f
    canvas.gcText(title, 0f, centerY - 35f, 38f, primary, isBold = true, maxWidth = width, alignment = Layout.Alignment.ALIGN_CENTER)
    if (subTitle.isNotBlank()) {
        canvas.gcText(subTitle, 0f, centerY + 20f, 28f, secondary, maxWidth = width, alignment = Layout.Alignment.ALIGN_CENTER)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 412, heightDp = 412)
@Composable
fun CompactLayoutPreview() {
    val today = LocalDate.now().toString()
    val mockSnapshot = WidgetSnapshot.newBuilder().apply {
        currentWeek = 18
        style = com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto.newBuilder().apply {
            addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder()
                .setLightColor(0xFF007AFF.toLong()).build())
            addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder()
                .setLightColor(0xFFFF9500.toLong()).build())
        }.build()
        addCourses(WidgetCourseProto.newBuilder().setName("高等数学").setTeacher("李老师").setStartTime("08:30:00").setEndTime("20:05:00").setPosition("教3-101").setColorInt(0).setDate(today).build())
        addCourses(WidgetCourseProto.newBuilder().setName("大学物理实验").setTeacher("王教授").setStartTime("10:20:00").setEndTime("21:55:00").setPosition("物理实验室").setColorInt(1).setDate(today).build())
    }.build()

    GlanceTheme {
        CompactLayout(snapshot = mockSnapshot)
    }
}