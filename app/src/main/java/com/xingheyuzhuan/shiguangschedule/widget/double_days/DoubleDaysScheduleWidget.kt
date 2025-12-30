package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.WidgetStateDefinition

class DoubleDaysScheduleWidget : GlanceAppWidget() {

    // 关键：指定快照状态定义
    override val stateDefinition = WidgetStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // 从 DataStore 中直接获取当前快照
            val snapshot = currentState<WidgetSnapshot>()

            GlanceTheme {
                DoubleDaysLayout(snapshot = snapshot)
            }
        }
    }
}