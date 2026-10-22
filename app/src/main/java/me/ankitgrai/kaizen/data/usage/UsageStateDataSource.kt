package me.ankitgrai.kaizen.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

class UsageStateDataSource (
    private val context: Context
){
    private val appsOpsManager =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasUsageAccess(): Boolean{
        return appsOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }
    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
    fun getTodayUsage(): List<AppUsageInfo> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val startOfDay = today
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val now = System.currentTimeMillis()

        // Read events from before midnight so an activity that
        // was already running can be handled correctly.
        val queryStart = startOfDay - 24 * 60 * 60 * 1000L

        val events = usageStatsManager.queryEvents(queryStart, now)
        val event = UsageEvents.Event()

        val activeSince = mutableMapOf<String, Long>()
        val durations = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val packageName = event.packageName ?: continue

            val isForegroundEvent =
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED

            val isBackgroundEvent =
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                        event.eventType == UsageEvents.Event.ACTIVITY_STOPPED

            when {
                isForegroundEvent -> {
                    // Close an earlier interval for this package if
                    // another foreground event arrived unexpectedly.
                    val previousStart = activeSince.put(
                        packageName,
                        event.timeStamp
                    )

                    if (previousStart != null) {
                        val intervalStart = maxOf(previousStart, startOfDay)
                        val intervalEnd = minOf(event.timeStamp, now)

                        if (intervalEnd > intervalStart) {
                            durations[packageName] =
                                (durations[packageName] ?: 0L) +
                                        (intervalEnd - intervalStart)
                        }
                    }
                }

                isBackgroundEvent -> {
                    val previousStart = activeSince.remove(packageName)

                    if (previousStart != null) {
                        val intervalStart = maxOf(previousStart, startOfDay)
                        val intervalEnd = minOf(event.timeStamp, now)

                        if (intervalEnd > intervalStart) {
                            durations[packageName] =
                                (durations[packageName] ?: 0L) +
                                        (intervalEnd - intervalStart)
                        }
                    }
                }
            }
        }

        // Include intervals that are still open at the time of the query.
        for ((packageName, foregroundStart) in activeSince) {
            val intervalStart = maxOf(foregroundStart, startOfDay)

            if (now > intervalStart) {
                durations[packageName] =
                    (durations[packageName] ?: 0L) +
                            (now - intervalStart)
            }
        }

        return durations
            .filterValues { it > 0L }
            .map { (packageName, duration) ->
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    totalTimeInForeground = duration
                )
            }
            .sortedByDescending { it.totalTimeInForeground }
    }
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                packageName,
                0
            )

            context.packageManager
                .getApplicationLabel(appInfo)
                .toString()
        } catch (_: Exception) {
            packageName
        }
    }
}