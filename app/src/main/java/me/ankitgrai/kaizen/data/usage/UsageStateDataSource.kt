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
import java.time.temporal.ChronoUnit

internal val NON_SCREEN_TIME = setOf(
    "com.samsung.android.emergency",
    "com.samsung.android.incallui",
    "com.samsung.android.dialer",
    "com.android.phone",
    "com.android.systemui"
)

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
            windowStartMillis - DAY_MILLIS,
            now
        ) ?: return emptyObservedUsage()

        val slices = UsageSliceAccumulator(zone, windowStart, today, windowStartMillis, now)
        slices.consume(events)
        return slices.toObservedUsage()
    }
}

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

private fun emptyObservedUsage() = ObservedUsage(null, emptyMap(), emptyList())

private class UsageSliceAccumulator(
    private val zone: ZoneId,
    private val windowStart: LocalDate,
    private val today: LocalDate,
    private val windowStartMillis: Long,
    private val now: Long,
) {
    private var screenOn = false
    private var resumedPackage: String? = null
    private var countingSince: Long? = null
    private var oldestEvent: Long? = null
    private val deviceSlices = mutableMapOf<LocalDate, Long>()
    private val hourSlices = mutableMapOf<Pair<LocalDate, Int>, Long>()
    private val appSlices = mutableMapOf<Pair<LocalDate, String>, Long>()

    fun consume(events: UsageEvents) {
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            onEvent(event)
        }
        addElapsed(now)
    }

    fun toObservedUsage(): ObservedUsage {
        val oldest = oldestEvent ?: return emptyObservedUsage()
        val earliest = maxOf(
            Instant.ofEpochMilli(oldest).atZone(zone).toLocalDate(),
            windowStart
        )
        if (earliest.isAfter(today)) return emptyObservedUsage()
        return ObservedUsage(
            earliestObserved = earliest,
            deviceTotals = deviceTotals(earliest),
            appTotals = appTotals(earliest),
            hourTotals = hourTotals(earliest),
        )
    }

    private fun onEvent(event: UsageEvents.Event) {
        val time = event.timeStamp
        rememberOldest(time)
        when (event.eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> startScreen(time)
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> stopScreen(time)
            UsageEvents.Event.ACTIVITY_RESUMED -> resume(event.packageName, time)
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> pause(event.packageName, time)
        }
    }

    private fun rememberOldest(time: Long) {
        val oldest = oldestEvent
        if (oldest == null || time < oldest) oldestEvent = time
    }

    private fun startScreen(time: Long) {
        screenOn = true
        maybeStart(time)
    }

    private fun stopScreen(time: Long) {
        addElapsed(time)
        screenOn = false
    }

    private fun resume(packageName: String, time: Long) {
        addElapsed(time)
        resumedPackage = packageName
        maybeStart(time)
    }

    private fun pause(packageName: String, time: Long) {
        if (packageName != resumedPackage) return
        addElapsed(time)
        resumedPackage = null
    }

    private fun maybeStart(at: Long) {
        if (screenOn && resumedPackage != null && countingSince == null) {
            countingSince = at
        }
    }

    private fun addElapsed(until: Long) {
        val since = countingSince ?: return
        val pkg = resumedPackage
        val from = maxOf(since, windowStartMillis)
        val to = minOf(until, now)
        if (pkg != null && to > from) walkSlices(pkg, from, to)
        countingSince = null
    }

    private fun walkSlices(pkg: String, from: Long, to: Long) {
        var cursor = from
        while (cursor < to) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sliceEnd = minOf(to, nextMidnight)
            recordSlice(date, pkg, cursor, sliceEnd)
            cursor = sliceEnd
        }
    }

    private fun recordSlice(date: LocalDate, pkg: String, from: Long, to: Long) {
        val slice = to - from
        if (!countsAsScreenTime(date, pkg, slice)) return
        deviceSlices[date] = deviceSlices.getOrDefault(date, 0L) + slice
        val key = date to pkg
        appSlices[key] = appSlices.getOrDefault(key, 0L) + slice
        addHourSlices(hourSlices, zone, date, from, to)
    }

    private fun countsAsScreenTime(date: LocalDate, pkg: String, slice: Long): Boolean {
        if (slice <= 0) return false
        if (date.isBefore(windowStart) || date.isAfter(today)) return false
        return pkg !in NON_SCREEN_TIME
    }

    private fun deviceTotals(earliest: LocalDate): Map<LocalDate, Long> {
        val totals = mutableMapOf<LocalDate, Long>()
        var date = earliest
        while (!date.isAfter(today)) {
            totals[date] = deviceSlices.getOrDefault(date, 0L)
            date = date.plusDays(1)
        }
        return totals
    }

    private fun appTotals(earliest: LocalDate): List<AppDayUsage> {
        return appSlices
            .filter { (key, _) -> observedDay(key.first, earliest) }
            .map { (key, millis) ->
                AppDayUsage(
                    date = key.first,
                    packageName = key.second,
                    totalUsageMillis = millis,
                )
            }
    }

    private fun hourTotals(earliest: LocalDate): List<HourlyDeviceUsage> = buildList {
        var date = earliest
        while (!date.isAfter(today)) {
            addAll(hoursOn(date))
            date = date.plusDays(1)
        }
    }

    private fun hoursOn(date: LocalDate): List<HourlyDeviceUsage> {
        return (0..23).map { hour ->
            HourlyDeviceUsage(
                date = date,
                hour = hour,
                totalUsageMillis = hourSlices.getOrDefault(date to hour, 0L),
            )
        }
    }

    private fun observedDay(date: LocalDate, earliest: LocalDate): Boolean {
        return !date.isBefore(earliest) && !date.isAfter(today)
    }
}

private fun addHourSlices(
    hourSlices: MutableMap<Pair<LocalDate, Int>, Long>,
    zone: ZoneId,
    date: LocalDate,
    from: Long,
    to: Long
) {
    var cursor = from
    while (cursor < to) {
        val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
        val nextHour = zoned.truncatedTo(ChronoUnit.HOURS)
            .plusHours(1)
            .toInstant()
            .toEpochMilli()
        val hourEnd = minOf(to, nextHour)
        val amount = hourEnd - cursor
        if (amount > 0) {
            val key = date to zoned.hour
            hourSlices[key] = (hourSlices[key] ?: 0L) + amount
        }
        cursor = hourEnd
    }
}