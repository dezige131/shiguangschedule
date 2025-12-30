package com.xingheyuzhuan.shiguangschedule.widget.tiny

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets // 导入你的更新工具函数
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class TinyScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TinyScheduleWidget()

    // 定义一个协程作用域，用于在 Receiver 中执行异步任务
    private val scope = MainScope()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 启动原有的周期性同步任务
        WorkManagerHelper.schedulePeriodicWork(context)

        // 立即执行一次快照更新
        // 这样可以确保用户添加组件后，数据和颜色能立刻显示出来，而不是等下一次周期任务
        scope.launch {
            updateAllWidgets(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManagerHelper.cancelAllWork(context)
    }
}