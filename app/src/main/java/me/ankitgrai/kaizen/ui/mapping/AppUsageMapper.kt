package me.ankitgrai.kaizen.ui.mapping

import androidx.compose.ui.graphics.Color
import me.ankitgrai.kaizen.data.usage.AppUsageInfo
import me.ankitgrai.kaizen.data.usage.InstalledAppInfo
import me.ankitgrai.kaizen.domain.categorizeApp
import me.ankitgrai.kaizen.domain.formatDuration
import me.ankitgrai.kaizen.domain.initials
import me.ankitgrai.kaizen.ui.components.AppUsage

private val fallbackColors = listOf(
    Color(0xFFFF8178),
    Color(0xFFC49BFF),
    Color(0xFF8CB8FF),
    Color(0xFFA6D989),
    Color(0xFFFFC078),
)

fun appFallbackColor(packageName: String): Color =
    fallbackColors[(packageName.hashCode() and Int.MAX_VALUE) % fallbackColors.size]

fun AppUsageInfo.toAppUsage(totalDailyUsage: Long): AppUsage {
    val progress = if (totalDailyUsage > 0L) {
        totalTimeInForeground.toFloat() / totalDailyUsage
    } else {
        0f
    }
    return AppUsage(
        name = appName,
        category = categorizeApp(packageName, appName),
        duration = formatDuration(totalTimeInForeground),
        initials = initials(appName),
        color = appFallbackColor(packageName),
        usageProgress = progress.coerceIn(0f, 1f),
        packageName = packageName,
        totalUsageMillis = totalTimeInForeground,
    )
}

fun InstalledAppInfo.toUnusedApp(): AppUsage = AppUsage(
    name = appName,
    category = categorizeApp(packageName, appName),
    duration = formatDuration(0L),
    initials = initials(appName),
    color = appFallbackColor(packageName),
    usageProgress = 0f,
    packageName = packageName,
)
