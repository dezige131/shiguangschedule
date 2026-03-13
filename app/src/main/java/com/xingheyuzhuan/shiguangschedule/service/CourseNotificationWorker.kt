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


/**
 * 修改说明：
 * 1. 增加了对 ACTION_CANCEL_LIVE_PROGRESS 闹钟的设置，确保课程结束时通知自动消失。
 * 2. 优化了 setAlarmInternal，支持三种类型的闹钟：普通提醒、进度显示、进度取消。
 * 3. 确保了 cancelAllAlarms 能够完整清理这三种闹钟。
 */

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

        // 内部前缀，用于区分同一课程的不同闹钟
        private const val PREFIX_PROGRESS_SHOW = "progress_show_"
        private const val PREFIX_PROGRESS_CANCEL = "progress_cancel_"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker 任务已启动...")
        return try {
            val appSettings = appSettingsRepository.getAppSettings().first()

            if (!appSettings.reminderEnabled) {
                Log.i(TAG, "课程提醒功能已关闭，正在取消所有现有闹钟。")
                cancelAllAlarms()
                return Result.success()
            }

            val remindBeforeMinutes = appSettings.remindBeforeMinutes
            val today = LocalDate.now()
            val startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = today.plusDays(WIDGET_SYNC_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE)

            val coursesToRemind = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()

            // 重新设置前，先根据记录清理旧闹钟
            cancelAllAlarms()

            val zoneId = ZoneId.systemDefault()
            val now = LocalDateTime.now()

            for (course in coursesToRemind) {
                if (course.isSkipped) continue

                val courseDate = LocalDate.parse(course.date)
                val startDT = LocalDateTime.of(courseDate, LocalTime.parse(course.startTime))
                val endDT = LocalDateTime.of(courseDate, LocalTime.parse(course.endTime))

                // 1. 设置【提前提醒闹钟】
                val remindTime = startDT.minusMinutes(remindBeforeMinutes.toLong())
                if (remindTime.isAfter(now)) {
                    setAlarmInternal(
                        courseId = course.id,
                        triggerTime = remindTime.atZone(zoneId).toInstant().toEpochMilli(),
                        name = course.name,
                        position = course.position,
                        alarmType = AlarmType.REMIND
                    )
                }

                // 2. 设置【上课进度条显示闹钟】
                if (endDT.isAfter(now)) {
                    val triggerTime = if (startDT.isBefore(now)) {
                        System.currentTimeMillis() + 800 // 课已开始，补发
                    } else {
                        startDT.atZone(zoneId).toInstant().toEpochMilli()
                    }

                    setAlarmInternal(
                        courseId = course.id,
                        triggerTime = triggerTime,
                        name = course.name,
                        position = "",
                        alarmType = AlarmType.PROGRESS_SHOW,
                        startTime = startDT.atZone(zoneId).toInstant().toEpochMilli(),
                        endTime = endDT.atZone(zoneId).toInstant().toEpochMilli()
                    )

                    // 3. 设置【课程结束自动关闭通知的闹钟】（关键新增点！）
                    setAlarmInternal(
                        courseId = course.id,
                        triggerTime = endDT.atZone(zoneId).toInstant().toEpochMilli(),
                        name = course.name,
                        position = "",
                        alarmType = AlarmType.PROGRESS_CANCEL
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "闹钟设置任务失败！", e)
            Result.failure()
        }
    }

    /**
     * 闹钟类型枚举，方便管理
     */
    enum class AlarmType {
        REMIND,          // 普通提醒
        PROGRESS_SHOW,   // 进度条开始显示
        PROGRESS_CANCEL  // 进度条结束取消
    }

    private fun setAlarmInternal(
        courseId: String,
        triggerTime: Long,
        name: String,
        position: String,
        alarmType: AlarmType,
        startTime: Long = 0L,
        endTime: Long = 0L
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            when (alarmType) {
                AlarmType.PROGRESS_SHOW -> {
                    putExtra(CourseAlarmReceiver.EXTRA_DND_ACTION, CourseAlarmReceiver.ACTION_LIVE_PROGRESS)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, name)
                    putExtra("extra_start_time", startTime)
                    putExtra("extra_end_time", endTime)
                    data = Uri.parse("live_progress://$courseId")
                }
                AlarmType.PROGRESS_CANCEL -> {
                    // 调用 Receiver 中的取消逻辑
                    putExtra(CourseAlarmReceiver.EXTRA_DND_ACTION, CourseAlarmReceiver.ACTION_CANCEL_LIVE_PROGRESS)
                    data = Uri.parse("cancel_progress://$courseId")
                }
                AlarmType.REMIND -> {
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, courseId)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, name)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_POSITION, position)
                    data = Uri.parse("course_remind://$courseId")
                }
            }
        }

        // 不同的 RequestCode 确保同一课程的三个闹钟互不覆盖
        val requestCode = when (alarmType) {
            AlarmType.PROGRESS_SHOW -> abs(courseId.hashCode() + 1)
            AlarmType.PROGRESS_CANCEL -> abs(courseId.hashCode() + 2)
            AlarmType.REMIND -> abs(courseId.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

        // 记录 ID 到 SharedPreferences 用于后续清理
        val sharedPreferences = applicationContext.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val currentIds = sharedPreferences.getStringSet(KEY_ACTIVE_ALARM_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val recordId = when (alarmType) {
            AlarmType.PROGRESS_SHOW -> "$PREFIX_PROGRESS_SHOW$courseId"
            AlarmType.PROGRESS_CANCEL -> "$PREFIX_PROGRESS_CANCEL$courseId"
            AlarmType.REMIND -> courseId
        }
        currentIds.add(recordId)

        sharedPreferences.edit { putStringSet(KEY_ACTIVE_ALARM_IDS, currentIds) }
    }

    private fun cancelAllAlarms() {
        val sharedPreferences = applicationContext.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val activeIds = sharedPreferences.getStringSet(KEY_ACTIVE_ALARM_IDS, null)

        activeIds?.forEach { recordId ->
            val isProgressShow = recordId.startsWith(PREFIX_PROGRESS_SHOW)
            val isProgressCancel = recordId.startsWith(PREFIX_PROGRESS_CANCEL)

            val originalId = when {
                isProgressShow -> recordId.removePrefix(PREFIX_PROGRESS_SHOW)
                isProgressCancel -> recordId.removePrefix(PREFIX_PROGRESS_CANCEL)
                else -> recordId
            }

            val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
                data = when {
                    isProgressShow -> Uri.parse("live_progress://$originalId")
                    isProgressCancel -> Uri.parse("cancel_progress://$originalId")
                    else -> Uri.parse("course_remind://$originalId")
                }
            }

            val requestCode = when {
                isProgressShow -> abs(originalId.hashCode() + 1)
                isProgressCancel -> abs(originalId.hashCode() + 2)
                else -> abs(originalId.hashCode())
            }

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

        sharedPreferences.edit { remove(KEY_ACTIVE_ALARM_IDS) }
        Log.i(TAG, "所有本地记录的闹钟（提醒+显示+取消）已清理。")
    }
}