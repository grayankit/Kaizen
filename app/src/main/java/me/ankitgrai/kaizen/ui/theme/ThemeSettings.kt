package me.ankitgrai.kaizen.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

enum class ThemeMode(val storageKey: String, val label: String) {
    System("system", "System"),
    Light("light", "Light"),
    Dark("dark", "Dark");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.storageKey == key } ?: System
    }
}

class ThemeSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("kaizen_theme", Context.MODE_PRIVATE)

    var themeId by mutableStateOf(
        AppThemeId.fromKey(prefs.getString(KEY_THEME, AppThemeId.Material.storageKey).orEmpty())
    )
        private set

    var mode by mutableStateOf(
        ThemeMode.fromKey(prefs.getString(KEY_MODE, ThemeMode.System.storageKey).orEmpty())
    )
        private set

    var oled by mutableStateOf(prefs.getBoolean(KEY_OLED, false))
        private set

    fun selectTheme(theme: AppThemeId) {
        themeId = theme
        prefs.edit { putString(KEY_THEME, theme.storageKey) }
    }

    fun selectMode(value: ThemeMode) {
        mode = value
        prefs.edit { putString(KEY_MODE, value.storageKey) }
    }

    fun selectOled(enabled: Boolean) {
        oled = enabled
        prefs.edit { putBoolean(KEY_OLED, enabled) }
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_MODE = "mode"
        const val KEY_OLED = "oled"
    }
}
