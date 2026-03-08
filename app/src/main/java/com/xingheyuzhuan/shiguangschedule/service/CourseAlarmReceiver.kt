package com.xingheyuzhuan.shiguangschedule.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "course_notification_channel"
        const val LIVE_COURSE_CHANNEL_ID = "live_course_progress_channel"
        const val NOTIFICATION_ID_BASE = 1000
        const val LIVE_COURSE_NOTIFICATION_ID = 2026
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_POSITION = "course_position"
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_DND_ACTION = "extra_dnd_action"
        const val DND_ACTION_START = "dnd_action_start"
        const val DND_ACTION_END = "dnd_action_end"
        const val ACTION_LIVE_PROGRESS = "action_live_progress"
        const val ACTION_CANCEL_LIVE_PROGRESS = "action_cancel_live_progress"

        private const val ALARM_IDS_PREFS = "alarm_ids_prefs"
        private const val KEY_ACTIVE_ALARM_IDS = "active_alarm_ids"
        private const val TAG = "CourseAlarmReceiver"
        const val MODE_DND = "DND"
        const val MODE_SILENT = "SILENT"

        fun toggleMode(context: Context, enableMode: Boolean, modeType: String) {
            val audioManager = context.getSystemService<AudioManager>()
            val notificationManager = context.getSystemService<NotificationManager>()
            if (audioManager == null || notificationManager == null) return
            if (!notificationManager.isNotificationPolicyAccessGranted) return
            when (modeType) {
                MODE_DND -> {
                    notificationManager.setInterruptionFilter(
                        if (enableMode) NotificationManager.INTERRUPTION_FILTER_NONE
                        else NotificationManager.INTERRUPTION_FILTER_ALL
                    )
                }
                MODE_SILENT -> {
                    audioManager.ringerMode = if (enableMode) AudioManager.RINGER_MODE_SILENT
                    else AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, ">>> onReceive 触发！Action: ${intent?.action}, Data: ${intent?.data}")
        context?.let { ctx ->
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = ctx.applicationContext as? MyApplication
                    val appSettingsRepository = app?.appSettingsRepository ?: run {
                        Log.e(TAG, "无法获取 AppSettingsRepository")
                        return@launch
                    }

                    val appSettings = appSettingsRepository.getAppSettings().first()
                    val modeToUse = appSettings.autoControlMode
                    val dndAction = intent?.getStringExtra(EXTRA_DND_ACTION)

                    if (!dndAction.isNullOrEmpty()) {
                        when (dndAction) {
                            DND_ACTION_START -> {
                                Log.i(TAG, "执行上课模式开启 (DND/静音)")
                                toggleMode(ctx, true, modeToUse)
                            }
                            DND_ACTION_END -> {
                                Log.i(TAG, "执行下课模式恢复")
                                toggleMode(ctx, false, modeToUse)
                                ctx.getSystemService<NotificationManager>()?.cancel(LIVE_COURSE_NOTIFICATION_ID)
                                DndSchedulerWorker.enqueueWork(ctx)
                            }
                            ACTION_LIVE_PROGRESS -> {
                                val startTime = intent.getLongExtra("extra_start_time", 0L)
                                val endTime = intent.getLongExtra("extra_end_time", 0L)
                                val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "未知课程"

                                if (startTime != 0L && endTime != 0L) {
                                    val nm = ctx.getSystemService<NotificationManager>()
                                    val isPromotedSupported = if (Build.VERSION.SDK_INT >= 36) {
                                        nm?.canPostPromotedNotifications() == true
                                    } else false

                                    Log.i(TAG, "触发实时进度条显示: $courseName, 提拔权限: $isPromotedSupported")
                                    showLiveProgressNotification(ctx, courseName, startTime, endTime, isPromotedSupported)
                                }
                            }
                            ACTION_CANCEL_LIVE_PROGRESS -> {
                                Log.i(TAG, "课程已准时结束，正在清理实时进度通知")
                                ctx.getSystemService<NotificationManager>()?.cancel(LIVE_COURSE_NOTIFICATION_ID)
                            }
                        }
                    } else {
                        // 普通提醒逻辑
                        val courseName = intent?.getStringExtra(EXTRA_COURSE_NAME) ?: ctx.getString(R.string.notification_unknown_course)
                        val coursePosition = intent?.getStringExtra(EXTRA_COURSE_POSITION) ?: ctx.getString(R.string.notification_unknown_position)
                        val courseIdString = intent?.getStringExtra(EXTRA_COURSE_ID)
                        Log.d(TAG, "普通提醒触发: $courseName")
                        if (!courseIdString.isNullOrEmpty()) {
                            val notificationId = courseIdString.hashCode() and 0x7fffffff
                            showNotification(ctx, notificationId, courseName, coursePosition)
                            removeAlarmIdFromPrefs(ctx, courseIdString)
                            updateAllWidgets(ctx)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onReceive 异常", e)
                } finally {
                    pendingResult.finish()
                    Log.d(TAG, "<<< onReceive 完成")
                }
            }
        }
    }

    private fun showLiveProgressNotification(context: Context, name: String, start: Long, end: Long, isPromoted: Boolean) {
        val nm = context.getSystemService<NotificationManager>() ?: return

        if (nm.getNotificationChannel(LIVE_COURSE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                LIVE_COURSE_CHANNEL_ID,
                context.getString(R.string.notification_channel_live_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc_live_progress)
            }
            nm.createNotificationChannel(channel)
        }

        val now = System.currentTimeMillis()
        val total = (end - start).coerceAtLeast(1L)
        val progress = (((now - start).toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
        val remainingMinutes = ((end - now) / 60000).coerceAtLeast(0)

        val fullTemplateText = context.getString(R.string.notification_live_content_template, remainingMinutes)
        val displayName = name.ifEmpty { context.getString(R.string.notification_default_course_name) }

        val builder = NotificationCompat.Builder(context, LIVE_COURSE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayName)
            .setContentText(fullTemplateText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setWhen(end)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(fullTemplateText)
        }

        nm.notify(LIVE_COURSE_NOTIFICATION_ID, builder.build())
    }

    private fun showNotification(context: Context, courseId: Int, name: String, position: String) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.item_course_reminder),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc_course_alert)
            }
            nm.createNotificationChannel(channel)
        }
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(context.getString(R.string.notification_title_course_alert))
            .setContentText("$name - $position")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID_BASE + courseId, notification)
    }

    private fun removeAlarmIdFromPrefs(context: Context, courseId: String) {
        val sp = context.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val currentIds = sp.getStringSet(KEY_ACTIVE_ALARM_IDS, null)?.toMutableSet()
        if (currentIds != null) {
            currentIds.remove(courseId)
            sp.edit { putStringSet(KEY_ACTIVE_ALARM_IDS, currentIds) }
        }
    }
}