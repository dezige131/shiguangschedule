package com.xingheyuzhuan.shiguangschedule.widget.large

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
import java.util.Locale
import java.time.format.TextStyle as LocalDateTextStyle

@Composable
fun LargeLayout(snapshot: WidgetSnapshot) {
    val context = LocalContext.current

    // 1. 数据筛选与逻辑处理
    val allCourses = snapshot.coursesList
    val currentWeek = if (snapshot.currentWeek == 0) null else snapshot.currentWeek
    val styleProto = snapshot.style
    val now = LocalTime.now()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val todayRemaining = allCourses.filter { (it.date == today.toString() || it.date.isBlank()) && !it.isSkipped && LocalTime.parse(it.endTime) > now }
    val tomorrowCourses = allCourses.filter { it.date == tomorrow.toString() && !it.isSkipped }

    val isVacation = currentWeek == null
    val isShowingTomorrow = !isVacation && todayRemaining.isEmpty() && tomorrowCourses.isNotEmpty()
    val displayCourses = if (isShowingTomorrow) tomorrowCourses.take(6) else todayRemaining.take(6)
    val totalRemaining = if (isShowingTomorrow) tomorrowCourses.size else todayRemaining.size
    val displayDate = if (isShowingTomorrow) tomorrow else today

    val designW = 918f
    val designH = 680f
    val safePadding = 42f
    val contentW = designW - (safePadding * 2)
    val contentH = designH - (safePadding * 2)

    val headerH = 60f
    val listStartY = headerH + 20f
    val rowHeight = 160f
    val colWidth = contentW / 2f

    val maskPrimary = android.graphics.Color.BLACK
    val maskSecondary = android.graphics.Color.argb(165, 0, 0, 0)
    val maskTertiary = android.graphics.Color.argb(130, 0, 0, 0)
    val maskDivider = android.graphics.Color.argb(45, 0, 0, 0)

    GCanvasViewport(
        designWidth = designW,
        designHeight = designH,
        snapshot = snapshot,
        cornerRadius = 55f,
        safePadding = safePadding,
        backgroundColor = WidgetColors.background,
        tintColor = WidgetColors.textPrimary,
        content = { metrics ->
            Box(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())) {
                if (!isVacation && displayCourses.isNotEmpty()) {
                    displayCourses.forEachIndexed { i, course ->
                        val row = i / 2
                        val col = i % 2
                        val x = col * colWidth
                        val y = listStartY + (row * rowHeight)
                        metrics.GCBox(x = x, y = y + 15f) {
                            CourseIndicator(0f, 0f, 130f, course.colorInt, metrics, styleProto)
                        }
                    }
                }
            }
        },
        onDraw = { canvas, _ ->
            val dateStr = "${displayDate.monthValue}.${displayDate.dayOfMonth} ${displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())}"
            val titleText = if (isShowingTomorrow) context.getString(R.string.widget_tomorrow_course_preview) else dateStr
            canvas.gcText(titleText, 0f, 0f, 30f, maskTertiary)

            if (currentWeek != null) {
                val weekStr = context.getString(R.string.status_current_week_format, currentWeek)
                canvas.gcText(weekStr, 0f, 0f, 30f, maskTertiary, maxWidth = contentW, alignment = Layout.Alignment.ALIGN_OPPOSITE)
            }

            if (isVacation) {
                drawLargeCenterMessage(
                    canvas = canvas,
                    w = contentW,
                    h = contentH,
                    text = context.getString(R.string.title_vacation),
                    color = maskPrimary,
                    subText = context.getString(R.string.widget_vacation_expecting),
                    subColor = maskSecondary
                )
            } else if (displayCourses.isEmpty()) {
                val msg = if (isShowingTomorrow) context.getString(R.string.widget_no_courses_tomorrow)
                else context.getString(R.string.widget_today_courses_finished)
                drawLargeCenterMessage(canvas, contentW, contentH, msg, maskPrimary)
            } else {
                displayCourses.forEachIndexed { i, course ->
                    val row = i / 2
                    val col = i % 2
                    val xBase = col * colWidth + 24f
                    val yBase = listStartY + (row * rowHeight) + 15f
                    val slotW = colWidth - 40f

                    // 绘制课程详情
                    drawLargeCourseItem(canvas, xBase, yBase, slotW, course, maskPrimary, maskSecondary, maskTertiary)

                    // 绘制分割线
                    if (row < 2 && (i + 2) < (displayCourses.size + (displayCourses.size % 2))) {
                        canvas.gcLine(
                            col * colWidth + 10f,
                            listStartY + (row + 1) * rowHeight + 5f,
                            (col + 1) * colWidth - 10f,
                            listStartY + (row + 1) * rowHeight + 5f,
                            1.2f,
                            maskDivider
                        )
                    }
                }
            }
            if (totalRemaining > 0) {
                val resId = if (isShowingTomorrow) R.string.widget_remaining_courses_format_tomorrow else R.string.widget_remaining_courses_format_today
                val footerText = context.getString(resId, totalRemaining)
                canvas.gcText(footerText, 0f, contentH - 30f, 26f, maskTertiary, maxWidth = contentW, alignment = Layout.Alignment.ALIGN_CENTER)
            }
        }
    )
}

/**
 * 内部绘制逻辑
 */
private fun drawLargeCourseItem(
    canvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    width: Float,
    course: WidgetCourseProto,
    primary: Int,
    secondary: Int,
    tertiary: Int
) {
    canvas.gcText(course.name, x, y, 34f, primary, isBold = true, maxWidth = width, maxLines = 1)

    val line2Y = y + 45f
    val timeStr = "${course.startTime.take(5)}-${course.endTime.take(5)}"
    if (course.position.isNotBlank()) {
        canvas.gcText(course.position, x, line2Y, 28f, secondary, maxWidth = width - 110f, maxLines = 1)
    }
    canvas.gcText(timeStr, x, line2Y, 28f, secondary, maxWidth = width, alignment = Layout.Alignment.ALIGN_OPPOSITE)

    if (course.teacher.isNotBlank()) {
        canvas.gcText(course.teacher, x, line2Y + 38f, 26f, tertiary, maxWidth = width, maxLines = 1)
    }
}


/**
 * 空状态居中显示：支持标题和副标题
 */
private fun drawLargeCenterMessage(
    canvas: android.graphics.Canvas,
    w: Float,
    h: Float,
    text: String,
    color: Int,
    subText: String = "",
    subColor: Int = color
) {
    val centerY = h / 2f
    if (subText.isBlank()) {
        canvas.gcText(text, 0f, centerY - 20f, 38f, color, isBold = true, maxWidth = w, alignment = Layout.Alignment.ALIGN_CENTER)
    } else {
        canvas.gcText(text, 0f, centerY - 40f, 40f, color, isBold = true, maxWidth = w, alignment = Layout.Alignment.ALIGN_CENTER)
        canvas.gcText(subText, 0f, centerY + 20f, 28f, subColor, maxWidth = w, alignment = Layout.Alignment.ALIGN_CENTER)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 918, heightDp = 680)
@Composable
fun LargeLayoutPreview() {
    val today = LocalDate.now().toString()
    val mockSnapshot = WidgetSnapshot.newBuilder().apply {
        currentWeek = 18
        style = com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto.newBuilder().apply {
            addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder()
                .setLightColor(0xFF007AFF.toLong()).build())
        }.build()
        addCourses(createMockCourse("计算机网络原理", "王教授", "08:30:00", "20:05:00", "理学楼 302", 0, today))
        addCourses(createMockCourse("高等数学(A)下", "李老师", "10:20:00", "22:00:00", "教1-101", 0, today))
        addCourses(createMockCourse("大学物理实验", "中心教师", "14:00:00", "23:35:00", "物理实验室", 0, today))
        addCourses(createMockCourse("形势与政策", "赵老师", "15:50:00", "18:25:00", "大礼堂", 1, today))
        addCourses(createMockCourse("英语听力", "Sarah", "18:30:00", "20:05:00", "外语楼 202", 0, today))
        addCourses(createMockCourse("体育(羽毛球)", "张教练", "20:20:00", "21:55:00", "体育馆", 0, today))
    }.build()

    GlanceTheme {
        LargeLayout(snapshot = mockSnapshot)
    }
}

private fun createMockCourse(name: String, teacher: String, start: String, end: String, pos: String, color: Int, date: String): WidgetCourseProto {
    return WidgetCourseProto.newBuilder().setName(name).setTeacher(teacher).setStartTime(start).setEndTime(end).setPosition(pos).setColorInt(color).setDate(date).build()
}