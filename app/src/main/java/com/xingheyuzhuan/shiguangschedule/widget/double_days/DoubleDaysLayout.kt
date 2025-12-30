package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.text.Layout
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.xingheyuzhuan.shiguangschedule.MainActivity
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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DoubleDaysLayout(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val allCourses = snapshot.coursesList
    val currentWeek = if (snapshot.currentWeek == 0) null else snapshot.currentWeek
    val styleProto = snapshot.style
    val now = LocalTime.now()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val rawTodayCourses = allCourses.filter { it.date == today.toString() || it.date.isBlank() }
    val rawTomorrowCourses = allCourses.filter { it.date == tomorrow.toString() }

    val totalRemainingToday = rawTodayCourses.count { !it.isSkipped && LocalTime.parse(it.endTime) > now }
    val totalTomorrow = rawTomorrowCourses.count { !it.isSkipped }

    val effectiveTodayCourses = rawTodayCourses.filter { !it.isSkipped && LocalTime.parse(it.endTime) > now }.take(2)
    val effectiveTomorrowCourses = rawTomorrowCourses.filter { !it.isSkipped }.take(2)

    GCanvasViewport(
        designWidth = 918f,
        designHeight = 412f,
        snapshot = snapshot,
        cornerRadius = 55f,
        safePadding = 42f,
        backgroundColor = WidgetColors.background,
        tintColor = GlanceTheme.colors.onSurface,
        content = { metrics ->
            val listStartY = 85f
            val centerX = (918f - 84f) / 2f

            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                if (currentWeek != null) {
                    // 今日指示器
                    effectiveTodayCourses.forEachIndexed { index, course ->
                        val yItem = listStartY + (index * 110f)
                        metrics.GCBox(x = 12f, y = yItem + 10f) {
                            CourseIndicator(0f, 0f, 85f, course.colorInt, metrics, styleProto)
                        }
                    }

                    // 明日指示器
                    effectiveTomorrowCourses.forEachIndexed { index, course ->
                        val yItem = listStartY + (index * 110f)
                        metrics.GCBox(x = centerX + 32f, y = yItem + 10f) {
                            CourseIndicator(0f, 0f, 85f, course.colorInt, metrics, styleProto)
                        }
                    }
                }
            }
        },
        onDraw = { canvas, _ ->
            val mask = android.graphics.Color.WHITE
            val maskHint = android.graphics.Color.argb(160, 255, 255, 255)
            val maskDivider = android.graphics.Color.argb(50, 255, 255, 255)

            val contentWidth = 918f - 84f
            val centerX = contentWidth / 2f
            val netHeight = 412f - 84f

            if (currentWeek != null) {
                // 顶部周次统计
                val weekText = context.getString(R.string.status_current_week_format, currentWeek)
                canvas.gcText(weekText, contentWidth - 200f, 0f, 30f, maskHint, maxWidth = 200f, alignment = Layout.Alignment.ALIGN_OPPOSITE)

                // 中间分割线
                canvas.gcLine(centerX + 10f, 40f, centerX + 10f, netHeight - 40f, 1.5f, maskDivider)

                // 左侧列：今日
                drawColumn(
                    canvas, context, 0f, centerX - 30f, netHeight,
                    context.getString(R.string.widget_title_today), today,
                    effectiveTodayCourses, totalRemainingToday, true, mask, maskHint, maskDivider
                )

                // 右侧列：明日
                drawColumn(
                    canvas, context, centerX + 20f, centerX - 30f, netHeight,
                    context.getString(R.string.widget_title_tomorrow), tomorrow,
                    effectiveTomorrowCourses, totalTomorrow, false, mask, maskHint, maskDivider
                )
            }else {
                val centerY = netHeight / 2f

                canvas.gcText(
                    text = context.getString(R.string.title_vacation),
                    x = 0f,
                    y = centerY - 45f,
                    size = 42f,
                    color = mask,
                    isBold = true,
                    maxWidth = contentWidth,
                    alignment = Layout.Alignment.ALIGN_CENTER
                )

                canvas.gcText(
                    text = context.getString(R.string.widget_vacation_expecting),
                    x = 0f,
                    y = centerY + 20f,
                    size = 30f,
                    color = maskHint,
                    maxWidth = contentWidth,
                    alignment = Layout.Alignment.ALIGN_CENTER
                )
            }
        }
    )
}

private fun drawColumn(
    canvas: android.graphics.Canvas,
    context: android.content.Context,
    x: Float,
    maxWidth: Float,
    totalHeight: Float,
    title: String,
    date: LocalDate,
    courses: List<WidgetCourseProto>,
    totalRemaining: Int,
    isToday: Boolean,
    primaryMask: Int,
    hintMask: Int,
    dividerMask: Int
) {
    val headerY = 25f
    val listStartY = 85f
    val footerY = totalHeight - 29f

    canvas.gcText(title, x, headerY, 38f, primaryMask, isBold = true)
    val dateStr = date.format(DateTimeFormatter.ofPattern("M.dd E", Locale.CHINA))
    canvas.gcText(dateStr, x + 100f, headerY + 4f, 28f, hintMask)

    if (totalRemaining == 0) {
        val emptyText = context.getString(R.string.text_no_course)
        canvas.gcText(
            text = emptyText,
            x = x,
            y = totalHeight / 2f - 20f,
            size = 34f,
            color = hintMask,
            maxWidth = maxWidth,
            alignment = Layout.Alignment.ALIGN_CENTER
        )
    } else {
        courses.forEachIndexed { i, course ->
            val yItem = listStartY + (i * 110f)
            drawCourseText(canvas, x + 35f, yItem, maxWidth - 35f, course, primaryMask, hintMask)

            if (i == 0 && courses.size > 1) {
                canvas.gcLine(x + 20f, yItem + 105f, x + maxWidth, yItem + 105f, 1f, dividerMask)
            }
        }
        // 4. 底部状态
        val resId = if (isToday) R.string.widget_remaining_courses_format_today else R.string.widget_remaining_courses_format_tomorrow
        val footerText = context.getString(resId, totalRemaining)
        canvas.gcText(
            text = footerText,
            x = x,
            y = footerY,
            size = 24f,
            color = hintMask,
            maxWidth = maxWidth,
            alignment = Layout.Alignment.ALIGN_CENTER
        )
    }
}

private fun drawCourseText(
    canvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    width: Float,
    course: WidgetCourseProto,
    primaryMask: Int,
    hintMask: Int
) {
    canvas.gcText(course.name, x, y - 2f, 34f, primaryMask, isBold = true, maxWidth = width, maxLines = 1)


    if (course.position.isNotBlank()) {
        canvas.gcText(
            text = course.position,
            x = x,
            y = y + 42f,
            size = 26f,
            color = hintMask,
            maxWidth = width - 150f,
            maxLines = 1,
            alignment = Layout.Alignment.ALIGN_NORMAL
        )
    }

    val timeInfo = "${course.startTime.take(5)}-${course.endTime.take(5)}"
    canvas.gcText(
        text = timeInfo,
        x = x,
        y = y + 42f,
        size = 26f,
        color = hintMask,
        maxWidth = width,
        maxLines = 1,
        alignment = Layout.Alignment.ALIGN_OPPOSITE
    )

    if (course.teacher.isNotBlank()) {
        canvas.gcText(course.teacher, x, y + 74f, 26f, hintMask, maxWidth = width, maxLines = 1)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 918, heightDp = 412)
@Composable
fun DoubleDaysLayoutPreview() {
    val today = LocalDate.now().toString()
    val mockSnapshot = WidgetSnapshot.newBuilder().apply {
        currentWeek = 18
        style = com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto.newBuilder().apply {
            addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder()
                .setLightColor(0xFF007AFF.toLong()).build())
        }.build()
        addCourses(WidgetCourseProto.newBuilder().setName("计算机网络原理").setTeacher("张教授").setStartTime("08:30:00").setEndTime("23:00:00").setPosition("理学楼 302计算机网络原理计算机网络原理").setColorInt(0).setDate(today).build())
        addCourses(WidgetCourseProto.newBuilder().setName("计算机网络原理").setTeacher("张教授").setStartTime("08:30:00").setEndTime("23:00:00").setPosition("理学楼 302").setColorInt(1).setDate(today).build())
    }.build()

    GlanceTheme {
        DoubleDaysLayout(snapshot = mockSnapshot)
    }
}