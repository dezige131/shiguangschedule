package com.xingheyuzhuan.shiguangschedule.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CourseNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository
) : CoroutineWorker(appContext, workerParams) {

    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val WIDGET_SYNC_DAYS = 7L
        private const val TAG = "CourseNotificationWorker"
        private const val ALARM_IDS_PREFS = "alarm_ids_prefs"
        private const val KEY_ACTIVE_ALARM_IDS = "active_alarm_ids"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker 开始同步课程闹钟...")
        return try {
            val appSettings = appSettingsRepository.getAppSettings().first()

            if (!appSettings.reminderEnabled) {
                Log.i(TAG, "提醒功能关闭，清空所有闹钟。")
                cancelAllAlarms()
                return Result.success()
            }

            val remindBeforeMinutes = appSettings.remindBeforeMinutes
            val today = LocalDate.now()
            val startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = today.plusDays(WIDGET_SYNC_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE)

            val coursesToRemind = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()

            // 1. 设置前先清理旧闹钟
            cancelAllAlarms()

            val zoneId = ZoneId.systemDefault()
            val now = LocalDateTime.now()

            for (course in coursesToRemind) {
                if (course.isSkipped) continue

                val courseDate = LocalDate.parse(course.date)
                val startDT = LocalDateTime.of(courseDate, LocalTime.parse(course.startTime))

                // 计算触发时间点
                val remindTime = startDT.minusMinutes(remindBeforeMinutes.toLong())

                // 2. 只为未来的提醒点设置闹钟
                if (remindTime.isAfter(now)) {
                    setAlarmInternal(
                        courseId = course.id,
                        triggerTime = remindTime.atZone(zoneId).toInstant().toEpochMilli(),
                        name = course.name,
                        position = course.position,
                        teacher = course.teacher
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "闹钟同步失败", e)
            Result.failure()
        }
    }

    private fun setAlarmInternal(
        courseId: String,
        triggerTime: Long,
        name: String,
        position: String,
        teacher: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            action = "com.xingheyuzhuan.shiguangschedule.ACTION_COURSE_REMIND"

            putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, courseId)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, name)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_POSITION, position)
            putExtra(CourseAlarmReceiver.EXTRA_COURSE_TEACHER, teacher)

            // 关键：data 唯一确保闹钟不被覆盖
            data = Uri.parse("course_remind://$courseId")
        }

        val requestCode = abs(courseId.hashCode())

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

        val sp = applicationContext.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val currentIds = sp.getStringSet(KEY_ACTIVE_ALARM_IDS, null)?.toMutableSet() ?: mutableSetOf()
        currentIds.add(courseId)
        sp.edit { putStringSet(KEY_ACTIVE_ALARM_IDS, currentIds) }
    }

    private fun cancelAllAlarms() {
        val sp = applicationContext.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val activeIds = sp.getStringSet(KEY_ACTIVE_ALARM_IDS, null)

        activeIds?.forEach { courseId ->
            val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
                data = Uri.parse("course_remind://$courseId")
            }
            val requestCode = abs(courseId.hashCode())
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        sp.edit { remove(KEY_ACTIVE_ALARM_IDS) }
        Log.i(TAG, "所有旧闹钟已清理。")
    }
}