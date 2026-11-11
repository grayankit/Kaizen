package me.ankitgrai.kaizen.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlin.math.roundToInt
import me.ankitgrai.kaizen.data.reel.ReelApps
import me.ankitgrai.kaizen.widget.WidgetUpdater

class ScreenTimeSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var targetMinutes by mutableIntStateOf(
        prefs.getInt(KEY_TARGET_MINUTES, DEFAULT_TARGET_MINUTES)
            .let(::snapTargetMinutes)
    )
        private set

    var reelApps by mutableStateOf(
        parseReelApps(prefs.getString(KEY_REEL_APPS, null))
    )
        private set

    val targetMillis: Long
        get() = targetMinutes * 60_000L

    fun selectTargetMinutes(minutes: Int) {
        val snapped = snapTargetMinutes(minutes)
        targetMinutes = snapped
        prefs.edit { putInt(KEY_TARGET_MINUTES, snapped) }
    }

    fun selectReelApps(apps: Set<String>) {
        reelApps = apps
        prefs.edit { putString(KEY_REEL_APPS, apps.joinToString(",")) }
        WidgetUpdater.publishReel(appContext)
    }

    fun pruneMissingReelApps(installed: Set<String>) {
        if (!prefs.contains(KEY_REEL_APPS)) return
        val pruned = reelApps.filter { it in installed }.toSet()
        if (pruned != reelApps) selectReelApps(pruned)
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
        private const val KEY_REEL_APPS = "reel_apps"

        fun readTargetMillis(context: Context): Long {
            val minutes = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_TARGET_MINUTES, DEFAULT_TARGET_MINUTES)
            return snapTargetMinutes(minutes) * 60_000L
        }

        fun readReelApps(context: Context): Set<String> {
            val stored = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REEL_APPS, null)
            return parseReelApps(stored)
        }

        fun registerReelAppsListener(
            context: Context,
            onChange: () -> Unit,
        ): SharedPreferences.OnSharedPreferenceChangeListener {
            val preferences = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_REEL_APPS) onChange()
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            return listener
        }

        fun unregisterReelAppsListener(
            context: Context,
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(listener)
        }

        private fun parseReelApps(stored: String?): Set<String> {
            if (stored == null) return ReelApps.official
            return stored.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
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
