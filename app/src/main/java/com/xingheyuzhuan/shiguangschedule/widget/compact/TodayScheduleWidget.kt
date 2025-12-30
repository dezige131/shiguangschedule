package com.xingheyuzhuan.shiguangschedule.widget.compact

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.WidgetStateDefinition

class TodayScheduleWidget : GlanceAppWidget() {

    // 关键：连接 DataStore 状态
    override val stateDefinition = WidgetStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // 直接读取当前小组件实例关联的快照数据
            val snapshot = currentState<WidgetSnapshot>()

            GlanceTheme {
                CompactLayout(snapshot = snapshot)
            }
        }
    }
}