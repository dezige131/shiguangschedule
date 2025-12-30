package com.xingheyuzhuan.shiguangschedule.widget.moderate

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.WidgetStateDefinition

class ModerateScheduleWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    // 1. 必须指定状态定义，否则 currentState<WidgetSnapshot>() 会返回空
    override val stateDefinition = WidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // 2. 直接从快照状态中获取数据，不再使用数据库 Flow
            val snapshot = currentState<WidgetSnapshot>()

            GlanceTheme {
                // 将整个快照传给 Layout
                ModerateLayout(snapshot = snapshot)
            }
        }
    }
}