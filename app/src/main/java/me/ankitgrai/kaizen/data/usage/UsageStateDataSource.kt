package me.ankitgrai.kaizen.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.time.Instant
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

    /**
     * Screen-on time for the days Android's event log still contains.
     * A quiet observed day is a real zero. A day with no events is omitted.
     */
    fun observeRecentUsage(days: Int = 14): ObservedUsage {
        require(days > 0) { "Days must be greater than zero." }

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val windowStart = today.minusDays((days - 1).toLong())
        val windowStartMillis = windowStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(
            windowStartMillis - 24 * 60 * 60 * 1000L,
            now
        ) ?: return ObservedUsage(null, emptyMap(), emptyList())

        val event = UsageEvents.Event()
        var oldestEvent: Long? = null
        var screenOn = false
        var resumedPackage: String? = null
        var countingSince: Long? = null
        val deviceSlices = mutableMapOf<LocalDate, Long>()
        val appSlices = mutableMapOf<Pair<LocalDate, String>, Long>()

        fun addElapsed(until: Long) {
            val since = countingSince ?: return
            val pkg = resumedPackage
            val from = maxOf(since, windowStartMillis)
            val to = minOf(until, now)
            if (pkg != null && to > from) {
                var cursor = from
                while (cursor < to) {
                    val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
                    val nextMidnight = date.plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    val sliceEnd = minOf(to, nextMidnight)
                    val slice = sliceEnd - cursor
                    if (slice > 0 && !date.isBefore(windowStart) && !date.isAfter(today)) {
                        if (pkg != context.packageName) {
                            deviceSlices[date] = (deviceSlices[date] ?: 0L) + slice
                        }
                        val key = date to pkg
                        appSlices[key] = (appSlices[key] ?: 0L) + slice
                    }
                    cursor = sliceEnd
                }
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
            if (oldestEvent == null || time < oldestEvent) {
                oldestEvent = time
            }
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

        val oldest = oldestEvent ?: return ObservedUsage(null, emptyMap(), emptyList())
        val earliest = maxOf(
            Instant.ofEpochMilli(oldest).atZone(zone).toLocalDate(),
            windowStart
        )
        if (earliest.isAfter(today)) {
            return ObservedUsage(null, emptyMap(), emptyList())
        }

        val deviceTotals = mutableMapOf<LocalDate, Long>()
        var date = earliest
        while (!date.isAfter(today)) {
            deviceTotals[date] = deviceSlices[date] ?: 0L
            date = date.plusDays(1)
        }
        val appTotals = appSlices
            .filterKeys { (day, _) -> !day.isBefore(earliest) && !day.isAfter(today) }
            .map { (key, millis) ->
                AppDayUsage(
                    date = key.first,
                    packageName = key.second,
                    totalUsageMillis = millis
                )
            }
        return ObservedUsage(
            earliestObserved = earliest,
            deviceTotals = deviceTotals,
            appTotals = appTotals
        )
    }
}