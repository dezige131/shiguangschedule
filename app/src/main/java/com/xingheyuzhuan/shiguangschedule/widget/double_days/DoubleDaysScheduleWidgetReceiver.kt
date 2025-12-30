package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DoubleDaysScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DoubleDaysScheduleWidget()
    private val scope = MainScope()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 1. 调度周期性同步任务
        WorkManagerHelper.schedulePeriodicWork(context)

        // 2. 关键：首次添加时立即生成快照，防止初始白屏
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
        // 最后一个小组件移除时清理后台任务
        WorkManagerHelper.cancelAllWork(context)
    }
}