package me.ankitgrai.kaizen.widget

import androidx.glance.appwidget.GlanceAppWidget

class ReelCountWidgetReceiver : RefreshingWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReelCountWidget()
}
