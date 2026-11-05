package me.ankitgrai.kaizen.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdater.refreshSoon(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdater.cancelRefresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdater.refreshSoon(context)
    }
}
