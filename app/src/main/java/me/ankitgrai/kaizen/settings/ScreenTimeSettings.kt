package me.ankitgrai.kaizen.settings

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlin.math.roundToInt

class ScreenTimeSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var targetMinutes by mutableIntStateOf(
        prefs.getInt(KEY_TARGET_MINUTES, DEFAULT_TARGET_MINUTES)
            .let(::snapTargetMinutes)
    )
        private set

    val targetMillis: Long
        get() = targetMinutes * 60_000L

    fun selectTargetMinutes(minutes: Int) {
        val snapped = snapTargetMinutes(minutes)
        targetMinutes = snapped
        prefs.edit { putInt(KEY_TARGET_MINUTES, snapped) }
    }

    companion object {
        const val DEFAULT_TARGET_MINUTES = 6 * 60
        const val MIN_TARGET_MINUTES = 30
        const val MAX_TARGET_MINUTES = 12 * 60
        const val STEP_MINUTES = 30

        val sliderSteps: Int
            get() = (MAX_TARGET_MINUTES - MIN_TARGET_MINUTES) / STEP_MINUTES - 1

        private const val PREFS_NAME = "kaizen_screen_time"
        private const val KEY_TARGET_MINUTES = "daily_target_minutes"
    }
}

fun snapTargetMinutes(minutes: Int): Int = snapTargetMinutes(minutes.toFloat())

fun snapTargetMinutes(minutes: Float): Int {
    val snapped = (minutes / ScreenTimeSettings.STEP_MINUTES).roundToInt() *
        ScreenTimeSettings.STEP_MINUTES
    return snapped.coerceIn(
        ScreenTimeSettings.MIN_TARGET_MINUTES,
        ScreenTimeSettings.MAX_TARGET_MINUTES
    )
}

fun formatScreenTimeTarget(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours > 0 && remaining > 0 -> "${hours}h ${remaining}m"
        hours > 0 -> "${hours}h"
        else -> "${remaining}m"
    }
}

val LocalScreenTimeSettings = staticCompositionLocalOf<ScreenTimeSettings?> { null }
