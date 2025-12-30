package com.xingheyuzhuan.shiguangschedule.widget.tiny

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.WidgetStateDefinition

class TinyScheduleWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    // 必须指定这个，否则 currentState 无法工作
    override val stateDefinition = WidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // 直接获取当前的快照状态
            val snapshot = currentState<WidgetSnapshot>()

            GlanceTheme {
                // 将整个快照传给 Layout
                TinyLayout(snapshot = snapshot)
            }
        }
    }
}