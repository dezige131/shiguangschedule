package com.xingheyuzhuan.shiguangschedule.widget.large

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.WidgetStateDefinition

class LargeScheduleWidget : GlanceAppWidget() {

    override val stateDefinition = WidgetStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val snapshot = currentState<WidgetSnapshot>()

            GlanceTheme {
                LargeLayout(snapshot = snapshot)
            }
        }
    }
}