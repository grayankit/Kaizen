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

        val startOfDay = LocalDate.now()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(
            startOfDay - 24 * 60 * 60 * 1000L,
            now
        ) ?: return emptyList()

        val event = UsageEvents.Event()
        val totals = mutableMapOf<String, Long>()

        var screenOn = false
        var resumedPackage: String? = null
        var countingSince: Long? = null

        fun addElapsed(until: Long) {
            val since = countingSince ?: return
            val pkg = resumedPackage ?: return
            val from = maxOf(since, startOfDay)
            val to = minOf(until, now)
            if (to > from) {
                totals[pkg] = (totals[pkg] ?: 0L) + (to - from)
            }
            countingSince = null
        }

        fun maybeStart(at: Long) {
            if (screenOn && resumedPackage != null && countingSince == null) {
                countingSince = at
            }
        }
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenOn = true
                    maybeStart(time)
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    addElapsed(time)
                    screenOn = false
                }

                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    addElapsed(time)
                    resumedPackage = event.packageName
                    maybeStart(time)
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (event.packageName == resumedPackage) {
                        addElapsed(time)
                        resumedPackage = null
                    }
                }
            }
        }
        addElapsed(now)

        return totals
            .filterValues { it > 0L }
            .map { (packageName, time) ->
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    totalTimeInForeground = time
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