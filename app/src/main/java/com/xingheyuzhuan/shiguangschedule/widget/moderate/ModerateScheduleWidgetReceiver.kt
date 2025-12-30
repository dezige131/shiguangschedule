package com.xingheyuzhuan.shiguangschedule.widget.moderate

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets // 确保导入了全局更新函数
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ModerateScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ModerateScheduleWidget()
    private val scope = MainScope()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManagerHelper.schedulePeriodicWork(context)

        // 关键：首次添加时立即生成快照数据
        scope.launch {
            updateAllWidgets(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManagerHelper.cancelAllWork(context)
    }
}