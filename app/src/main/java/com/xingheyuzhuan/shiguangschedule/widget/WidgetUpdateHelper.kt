package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetDatabase
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.scheduleGridStyleDataStore
import com.xingheyuzhuan.shiguangschedule.widget.compact.TodayScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.large.LargeScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.moderate.ModerateScheduleWidget
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyScheduleWidget
import kotlinx.coroutines.flow.first
import java.time.LocalDate

suspend fun updateAllWidgets(context: Context) {

    try {
        val widgetDb = WidgetDatabase.getDatabase(context)
        val repository = WidgetRepository(
            widgetCourseDao = widgetDb.widgetCourseDao(),
            widgetAppSettingsDao = widgetDb.widgetAppSettingsDao(),
            context = context
        )

        // --- 1. 计算日期范围 ---
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val todayStr = today.toString()
        val tomorrowStr = tomorrow.toString()

        val dbCourses = repository.getWidgetCoursesByDateRange(todayStr, tomorrowStr).first()

        // --- 2. 获取周数 ---
        val currentWeek = repository.getCurrentWeekFlow().first() ?: 0

        // --- 3. 获取样式并处理“颜色为空”的情况 ---
        val currentStyle = context.scheduleGridStyleDataStore.data.first()

        val finalStyleToSync = if (currentStyle.courseColorMapsCount == 0) {
            ScheduleGridStyle.DEFAULT.toProto()
        } else {
            currentStyle
        }

        // --- 4. 构造二进制快照对象 ---
        val snapshot = WidgetSnapshot.newBuilder().apply {
            this.currentWeek = currentWeek
            // 使用兜底后的样式，确保颜色索引对应得上
            this.style = finalStyleToSync

            dbCourses.forEach { course ->
                val courseProto = WidgetCourseProto.newBuilder()
                    .setId(course.id)
                    .setName(course.name)
                    .setTeacher(course.teacher)
                    .setPosition(course.position)
                    .setStartTime(course.startTime)
                    .setEndTime(course.endTime)
                    .setColorInt(course.colorInt) // 这里是索引
                    .setIsSkipped(course.isSkipped)
                    .setDate(course.date)
                    .build()
                addCourses(courseProto)
            }
        }.build()

        // --- 5. 分发状态给所有小组件 ---
        val manager = GlanceAppWidgetManager(context)
        val widgetClasses = listOf(
            TinyScheduleWidget::class.java,
            TodayScheduleWidget::class.java,
            ModerateScheduleWidget::class.java,
            DoubleDaysScheduleWidget::class.java,
            LargeScheduleWidget::class.java
        )

        widgetClasses.forEach { cls ->
            val glanceIds = manager.getGlanceIds(cls)
            if (glanceIds.isNotEmpty()) {
                glanceIds.forEach { id ->
                    updateAppWidgetState(context, WidgetStateDefinition, id) {
                        snapshot
                    }
                    // 强制 UI 重新 provideContent
                    cls.getDeclaredConstructor().newInstance().update(context, id)
                }
            }
        }

    } catch (e: Exception) {
        Log.e("WidgetUpdateHelper", "更新失败: ${e.stackTraceToString()}")
    }
}