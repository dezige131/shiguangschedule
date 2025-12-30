package com.xingheyuzhuan.shiguangschedule.widget.large

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LargeScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeScheduleWidget()
    private val scope = MainScope()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManagerHelper.schedulePeriodicWork(context)

        scope.launch {
            updateAllWidgets(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 当最后一个小组件实例被移除时，取消所有后台任务
        WorkManagerHelper.cancelAllWork(context)
    }
}