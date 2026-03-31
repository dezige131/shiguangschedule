package com.xingheyuzhuan.shiguangschedulemiuix.widget.double_days

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedulemiuix.MainActivity
import com.xingheyuzhuan.shiguangschedulemiuix.R
import com.xingheyuzhuan.shiguangschedulemiuix.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedulemiuix.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DoubleDaysNativeRenderer {

    fun render(context: Context, snapshot: WidgetSnapshot): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_double_days_native)

        // 状态彻底重置
        resetWidgetState(rv)

        // 点击跳转逻辑
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // 全局状态判断
        val currentWeek = if (snapshot.currentWeek <= 0) null else snapshot.currentWeek

        if (currentWeek == null) {
            rv.setViewVisibility(R.id.inner_content_card, View.GONE)
            rv.setViewVisibility(R.id.container_vacation, View.VISIBLE)
            rv.setTextViewText(R.id.tv_vacation_title, context.getString(R.string.title_vacation))
            rv.setTextViewText(
                R.id.tv_vacation_msg,
                context.getString(R.string.widget_vacation_expecting)
            )
            return rv
        }

        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_vacation, View.GONE)
        rv.setTextViewText(
            R.id.tv_current_week,
            context.getString(R.string.status_current_week_format, currentWeek)
        )

        val now = LocalTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val allCourses = snapshot.coursesList

        // 渲染左侧：今日
        val todayCourses = allCourses.filter { it.date == today.toString() || it.date.isBlank() }
        val remainingToday = todayCourses.filter {
            !it.isSkipped && try {
                LocalTime.parse(it.endTime) > now
            } catch (e: Exception) {
                true
            }
        }.sortedBy { it.startTime }

        renderColumn(
            context, rv,
            R.id.container_today, R.id.tv_today_date, R.id.tv_today_footer,
            R.id.empty_today_container,
            today, remainingToday.take(2), remainingToday.size,
            R.string.widget_remaining_courses_format_today, snapshot
        )

        // 渲染右侧：明日
        val tomorrowCourses = allCourses.filter { it.date == tomorrow.toString() }
        val effectiveTomorrow = tomorrowCourses.filter { !it.isSkipped }.sortedBy { it.startTime }

        renderColumn(
            context, rv,
            R.id.container_tomorrow, R.id.tv_tomorrow_date, R.id.tv_tomorrow_footer,
            R.id.empty_tomorrow_container,
            tomorrow, effectiveTomorrow.take(2), effectiveTomorrow.size,
            R.string.widget_remaining_courses_format_tomorrow, snapshot
        )

        return rv
    }

    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_vacation, View.GONE)
        rv.removeAllViews(R.id.container_today)
        rv.removeAllViews(R.id.container_tomorrow)
        rv.setViewVisibility(R.id.empty_today_container, View.GONE)
        rv.setViewVisibility(R.id.empty_tomorrow_container, View.GONE)
    }

    private fun renderColumn(
        context: Context,
        rootRv: RemoteViews,
        containerId: Int,
        dateId: Int,
        footerId: Int,
        emptyContainerId: Int,
        date: LocalDate,
        displayCourses: List<WidgetCourseProto>,
        totalCount: Int,
        footerResId: Int,
        snapshot: WidgetSnapshot
    ) {
        val prefix = if (date == LocalDate.now()) {
            context.getString(R.string.widget_title_today)
        } else {
            context.getString(R.string.widget_title_tomorrow)
        }
        val datePattern = date.format(DateTimeFormatter.ofPattern("M.dd E", Locale.getDefault()))
        rootRv.setTextViewText(dateId, "$prefix $datePattern")

        if (totalCount == 0) {
            // 无课：隐藏课程容器和页脚，显示居中容器
            rootRv.setViewVisibility(containerId, View.GONE)
            rootRv.setViewVisibility(emptyContainerId, View.VISIBLE)
            rootRv.setViewVisibility(footerId, View.GONE)
            val emptyTextViewId =
                if (date == LocalDate.now()) R.id.empty_today else R.id.empty_tomorrow
            rootRv.setTextViewText(emptyTextViewId, context.getString(R.string.text_no_course))
        } else {
            // 有课：显示课程容器和页脚，隐藏居中容器
            rootRv.setViewVisibility(containerId, View.VISIBLE)
            rootRv.setViewVisibility(emptyContainerId, View.GONE)
            rootRv.setViewVisibility(footerId, View.VISIBLE)
            rootRv.setTextViewText(footerId, context.getString(footerResId, totalCount))

            displayCourses.forEachIndexed { index, course ->
                val itemRv = RemoteViews(context.packageName, R.layout.widget_item_course_common)
                itemRv.setTextViewText(R.id.tv_course_name, course.name)
                itemRv.setTextViewText(R.id.tv_course_position, course.position)
                itemRv.setTextViewText(
                    R.id.tv_course_time,
                    "${course.startTime.take(5)}-${course.endTime.take(5)}"
                )

                if (course.teacher.isNotBlank()) {
                    itemRv.setViewVisibility(R.id.tv_course_teacher, View.VISIBLE)
                    itemRv.setTextViewText(R.id.tv_course_teacher, course.teacher)
                } else {
                    itemRv.setViewVisibility(R.id.tv_course_teacher, View.GONE)
                }

                val style = snapshot.style
                if (course.colorInt < style.courseColorMapsCount) {
                    val colorPair = style.getCourseColorMaps(course.colorInt)
                    itemRv.setInt(
                        R.id.course_indicator,
                        "setColorFilter",
                        colorPair.lightColor.toInt()
                    )
                    itemRv.setInt(
                        R.id.course_indicator_dark,
                        "setColorFilter",
                        colorPair.darkColor.toInt()
                    )
                }

                rootRv.addView(containerId, itemRv)

                if (index == 0 && displayCourses.size > 1) {
                    rootRv.addView(
                        containerId,
                        RemoteViews(context.packageName, R.layout.widget_divider_horizontal)
                    )
                }
            }
        }
    }
}