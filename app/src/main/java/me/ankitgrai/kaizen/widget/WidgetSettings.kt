package me.ankitgrai.kaizen.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.core.content.edit

object WidgetSettings {
    const val MIN_OPACITY_PERCENT = 0
    const val MAX_OPACITY_PERCENT = 100
    const val DEFAULT_OPACITY_PERCENT = 100

    private const val PREFS_NAME = "kaizen_widget"
    private const val KEY_OPACITY = "background_opacity_percent"
    private val loadLock = Any()
    private var loaded = false

    // Glance sessions stay alive and do not rerun provideGlance on update().
    // Widgets read this state during composition so a slider change republishes immediately.
    var opacityPercent by mutableStateOf(DEFAULT_OPACITY_PERCENT)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(loadLock) {
            if (loaded) return
            setOpacity(readStoredPercent(context))
            loaded = true
        }
    }

    fun readOpacityPercent(context: Context): Int {
        ensureLoaded(context)
        return opacityPercent
    }

    fun readOpacity(context: Context): Float = readOpacityPercent(context) / 100f

    fun writeOpacityPercent(context: Context, percent: Int) {
        val stored = percent.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT)
        ensureLoaded(context)
        setOpacity(stored)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_OPACITY, stored) }
    }

    private fun setOpacity(percent: Int) {
        Snapshot.withMutableSnapshot {
            opacityPercent = percent
        }
    }

    private fun readStoredPercent(context: Context): Int {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_OPACITY, DEFAULT_OPACITY_PERCENT)
        return stored.coerceIn(MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT)
    }
}
