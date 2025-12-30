package com.xingheyuzhuan.shiguangschedule.widget.moderate

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
fun ModerateLayout(snapshot: WidgetSnapshot) {
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
    val tomorrowCourses = allCourses.filter {
        it.date == tomorrow.toString() && !it.isSkipped
    }

    val isVacation = currentWeek == null
    val isShowingTomorrow = !isVacation && todayRemaining.isEmpty() && tomorrowCourses.isNotEmpty()

    val maxSlots = 4
    val displayCourses = if (isShowingTomorrow) tomorrowCourses.take(maxSlots) else todayRemaining.take(maxSlots)
    val totalCount = if (isShowingTomorrow) tomorrowCourses.size else todayRemaining.size
    val displayDate = if (isShowingTomorrow) tomorrow else today

    val designW = 918f
    val designH = 412f
    val safePadding = 42f
    val contentW = designW - (safePadding * 2)
    val contentH = designH - (safePadding * 2)

    val listStartY = 55f
    val columnWidth = contentW / 2f
    val rowHeight = 135f
    val textStartX = 20f

    val maskPrimary = android.graphics.Color.BLACK
    val maskSecondary = android.graphics.Color.argb(165, 0, 0, 0)
    val maskHint = android.graphics.Color.argb(140, 0, 0, 0)
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
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                if (!isVacation && displayCourses.isNotEmpty()) {
                    displayCourses.forEachIndexed { index, course ->
                        val col = index % 2
                        val row = index / 2
                        val xPos = col * columnWidth
                        val yPos = listStartY + (row * rowHeight)

                        metrics.GCBox(x = xPos, y = yPos) {
                            CourseIndicator(0f, 0f, 105f, course.colorInt, metrics, styleProto)
                        }
                    }
                }
            }
        },
        onDraw = { canvas, _ ->
            // --- 1. Header (使用 Hint 蒙版) ---
            val headerTitle = if (isShowingTomorrow) {
                context.getString(R.string.widget_tomorrow_course_preview)
            } else {
                "${displayDate.monthValue}.${displayDate.dayOfMonth} ${displayDate.dayOfWeek.getDisplayName(LocalDateTextStyle.SHORT, Locale.getDefault())}"
            }
            canvas.gcText(headerTitle, 0f, 0f, 28f, maskHint)

            if (currentWeek != null) {
                val weekStr = context.getString(R.string.status_current_week_format, currentWeek)
                canvas.gcText(weekStr, 0f, 0f, 28f, maskHint, maxWidth = contentW, alignment = Layout.Alignment.ALIGN_OPPOSITE)
            }

            // --- 2. 核心展示区域 ---
            when {
                isVacation -> drawCenterStatusMask(canvas, contentW, contentH, context.getString(R.string.title_vacation), context.getString(R.string.widget_vacation_expecting), maskPrimary, maskSecondary)

                displayCourses.isEmpty() -> {
                    val statusText = if (isShowingTomorrow) context.getString(R.string.widget_no_courses_tomorrow) else context.getString(R.string.widget_today_courses_finished)
                    drawCenterStatusMask(canvas, contentW, contentH, statusText, "", maskPrimary, maskSecondary)
                }

                else -> {
                    displayCourses.forEachIndexed { i, course ->
                        val col = i % 2
                        val row = i / 2
                        val xBase = col * columnWidth + textStartX
                        val yBase = listStartY + (row * rowHeight)
                        val slotWidth = columnWidth - textStartX - 15f

                        // 绘制课程详情 (内部区分 Primary 和 Secondary)
                        drawModerateCourseItemMask(canvas, xBase, yBase, slotWidth, course, maskPrimary, maskSecondary)

                        // 分割线 (使用最淡的 Divider 蒙版)
                        if (row == 0 && displayCourses.size > 2) {
                            canvas.gcLine(col * columnWidth + 10f, yBase + rowHeight - 8f, (col + 1) * columnWidth - 10f, yBase + rowHeight - 8f, 1f, maskDivider)
                        }
                    }

                    // --- 3. 底部计数 (使用 Hint 蒙版) ---
                    if (totalCount > 0) {
                        val resId = if (isShowingTomorrow) R.string.widget_remaining_courses_format_tomorrow else R.string.widget_remaining_courses_format_today
                        val footerText = context.getString(resId, totalCount)
                        canvas.gcText(footerText, 0f, contentH - 30f, 26f, maskHint, maxWidth = contentW, alignment = Layout.Alignment.ALIGN_CENTER)
                    }
                }
            }
        }
    )
}

/**
 * 绘制课程项形状蒙版：区分标题和详情的透明度
 */
private fun drawModerateCourseItemMask(
    canvas: android.graphics.Canvas,
    x: Float,
    y: Float,
    maxWidth: Float,
    course: WidgetCourseProto,
    primaryMask: Int,
    secondaryMask: Int
) {
    // 1. 课程名 (完全不透明)
    canvas.gcText(course.name, x, y - 6f, 34f, primaryMask, isBold = true, maxWidth = maxWidth, maxLines = 1)

    // 2. 地点与时间 (次级透明度)
    val subY = y + 42f
    val timeStr = "${course.startTime.take(5)}-${course.endTime.take(5)}"

    if (course.position.isNotBlank()) {
        canvas.gcText(course.position, x, subY, 26f, secondaryMask, maxWidth = maxWidth - 125f, maxLines = 1)
    }
    canvas.gcText(timeStr, x, subY, 26f, secondaryMask, maxWidth = maxWidth, alignment = Layout.Alignment.ALIGN_OPPOSITE)

    // 3. 教师 (次级透明度)
    if (course.teacher.isNotBlank()) {
        canvas.gcText(course.teacher, x, subY + 34f, 26f, secondaryMask, maxWidth = maxWidth, maxLines = 1)
    }
}

/**
 * 绘制中心状态形状蒙版
 */
private fun drawCenterStatusMask(
    canvas: android.graphics.Canvas,
    width: Float,
    height: Float,
    title: String,
    subTitle: String,
    primaryMask: Int,
    secondaryMask: Int
) {
    val centerY = height / 2f
    canvas.gcText(title, 0f, centerY - 35f, 38f, primaryMask, isBold = true, maxWidth = width, alignment = Layout.Alignment.ALIGN_CENTER)
    if (subTitle.isNotBlank()) {
        canvas.gcText(subTitle, 0f, centerY + 20f, 28f, secondaryMask, maxWidth = width, alignment = Layout.Alignment.ALIGN_CENTER)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 918, heightDp = 412)
@Composable
fun ModerateLayoutPreview() {
    val today = LocalDate.now().toString()
    val mockSnapshot = WidgetSnapshot.newBuilder().apply {
        currentWeek = 18
        style = com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto.newBuilder().apply {
            addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder().setLightColor(0xFF007AFF.toLong()).build())
            addCourseColorMaps(com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto.newBuilder().setLightColor(0xFFFF9500.toLong()).build())
        }.build()
        addCourses(WidgetCourseProto.newBuilder().setName("高等数学").setTeacher("李老师").setStartTime("08:30:00").setEndTime("20:05:00").setPosition("教3-101").setColorInt(0).setDate(today).build())
        addCourses(WidgetCourseProto.newBuilder().setName("大学物理").setTeacher("王教授").setStartTime("10:20:00").setEndTime("21:55:00").setPosition("物理实验室").setColorInt(1).setDate(today).build())
        addCourses(WidgetCourseProto.newBuilder().setName("计算机组成原理").setTeacher("赵老师").setStartTime("14:00:00").setEndTime("22:35:00").setPosition("实验楼402").setColorInt(0).setDate(today).build())
        addCourses(WidgetCourseProto.newBuilder().setName("形势与政策").setTeacher("孙老师").setStartTime("15:50:00").setEndTime("23:25:00").setPosition("大礼堂").setColorInt(1).setDate(today).build())
    }.build()

    GlanceTheme {
        ModerateLayout(snapshot = mockSnapshot)
    }
}