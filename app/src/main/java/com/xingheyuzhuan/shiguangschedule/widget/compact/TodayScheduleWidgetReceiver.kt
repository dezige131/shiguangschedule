package com.xingheyuzhuan.shiguangschedule.widget.compact

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class TodayScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayScheduleWidget()
    private val scope = MainScope()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 开启定时更新任务
        WorkManagerHelper.schedulePeriodicWork(context)

        scope.launch {
            try {
                updateAllWidgets(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 清理后台任务
        WorkManagerHelper.cancelAllWork(context)
    }
}