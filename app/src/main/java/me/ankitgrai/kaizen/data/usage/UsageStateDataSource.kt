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
    fun getAppUsageHistory(
        packageName: String,
        days: Int = 7
    ): List<DailyAppUsage> {
        require(days > 0) {
            "Days must be greater than zero"
        }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val totals = screenOnUsageByDay(
            startDate = today.minusDays((days - 1).toLong()),
            days = days,
            packageName = packageName
        )
        return totals.map { (date, time) ->
            DailyAppUsage(
                date = date,
                packageName = packageName,
                totalTimeInForeground = time
            )
        }
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

    fun getDailyDeviceUsage(days: Int = 90): List<DailyDeviceUsage> {
        require(days > 0) {
            "Days must be greater than zero."
        }
        val today = LocalDate.now(ZoneId.systemDefault())
        return screenOnUsageByDay(
            startDate = today.minusDays((days - 1).toLong()),
            days = days,
            packageName = null
        ).map { (date, time) ->
            DailyDeviceUsage(
                date = date,
                totalUsageMillis = time
            )
        }
    }

    /**
     * Screen-on time for each local day in [startDate, startDate + days).
     * [packageName] null counts whichever app is resumed, except this app.
     * A session that crosses midnight is split so each slice stays on its own date.
     */
    private fun screenOnUsageByDay(
        startDate: LocalDate,
        days: Int,
        packageName: String?
    ): Map<LocalDate, Long> {
        val zone = ZoneId.systemDefault()
        val rangeStart = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val totals = (0 until days).associate {
            startDate.plusDays(it.toLong()) to 0L
        }.toMutableMap()

        val events = usageStatsManager.queryEvents(
            rangeStart - 24 * 60 * 60 * 1000L,
            now
        ) ?: return totals

        val event = UsageEvents.Event()
        var screenOn = false
        var resumedPackage: String? = null
        var countingSince: Long? = null

        fun counts(pkg: String): Boolean {
            return if (packageName == null) {
                pkg != context.packageName
            } else {
                pkg == packageName
            }
        }

        fun addElapsed(until: Long) {
            val since = countingSince ?: return
            val pkg = resumedPackage
            val from = maxOf(since, rangeStart)
            val to = minOf(until, now)
            if (pkg != null && counts(pkg) && to > from) {
                var cursor = from
                while (cursor < to) {
                    val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
                    val nextMidnight = date.plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    val sliceEnd = minOf(to, nextMidnight)
                    if (date in totals) {
                        totals[date] = totals.getValue(date) + (sliceEnd - cursor)
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
        return totals.toSortedMap()
    }
}