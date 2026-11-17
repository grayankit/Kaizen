package me.ankitgrai.kaizen.widget

import android.content.Context
import androidx.core.content.edit

object WidgetSettings {
    const val MIN_OPACITY_PERCENT = 0
    const val MAX_OPACITY_PERCENT = 100
    const val DEFAULT_OPACITY_PERCENT = 100

    private const val PREFS_NAME = "kaizen_widget"
    private const val KEY_OPACITY = "background_opacity_percent"

    fun readOpacityPercent(context: Context): Int {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_OPACITY, DEFAULT_OPACITY_PERCENT)
        return stored.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT)
    }

    fun readOpacity(context: Context): Float = readOpacityPercent(context) / 100f

    fun writeOpacityPercent(context: Context, percent: Int) {
        val stored = percent.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_OPACITY, stored) }
    }
}
