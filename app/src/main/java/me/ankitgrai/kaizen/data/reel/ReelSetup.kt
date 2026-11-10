package me.ankitgrai.kaizen.data.reel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils

object ReelSetup {
    fun isEnabled(context: Context): Boolean {
        val resolver = context.contentResolver
        val accessibilityOn = Settings.Secure.getInt(
            resolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1
        if (!accessibilityOn) return false
        val enabled = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(context, ReelAccessibilityService::class.java)
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component == expected) return true
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, ReelAccessibilityService::class.java)
        val details = Intent(ACCESSIBILITY_DETAILS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, component.flattenToString())
        }
        if (!context.tryStart(details)) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    fun openBatterySettings(context: Context) {
        val uri = Uri.fromParts("package", context.packageName, null)
        val detail = Intent(POWER_USAGE_DETAIL).apply { data = uri }
        if (!context.tryStart(detail)) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = uri },
            )
        }
    }

    private fun Context.tryStart(intent: Intent): Boolean {
        return runCatching { startActivity(intent) }.isSuccess
    }

    private const val ACCESSIBILITY_DETAILS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    private const val POWER_USAGE_DETAIL = "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
}
