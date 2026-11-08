package me.ankitgrai.kaizen.data.reel

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

internal class ReelCounter(context: Context) {
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun countToday(targetApps: Set<String>): Map<String, Int> {
        if (targetApps.isEmpty()) return emptyMap()

        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = System.currentTimeMillis()

        val counts = mutableMapOf<String, Int>()
        val events = usm.queryEvents(start, end) ?: return emptyMap()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                && event.packageName in targetApps
            ) {
                counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
            }
        }
        return counts
    }
}
