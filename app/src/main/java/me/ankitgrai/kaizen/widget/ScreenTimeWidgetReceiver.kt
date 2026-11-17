package me.ankitgrai.kaizen.widget

import androidx.glance.appwidget.GlanceAppWidget

class ScreenTimeWidgetReceiver : RefreshingWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()
}
