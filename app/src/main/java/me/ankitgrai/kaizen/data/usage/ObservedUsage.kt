package me.ankitgrai.kaizen.data.usage

import java.time.LocalDate

data class AppDayUsage(
    val date: LocalDate,
    val packageName: String,
    val totalUsageMillis: Long
)

/**
 * Days the event log still covers.
 * [earliestObserved] is null when Android returned no events.
 * Dates before that were not seen and must not be written as zero.
 */
data class ObservedUsage(
    val earliestObserved: LocalDate?,
    val deviceTotals: Map<LocalDate, Long>,
    val appTotals: List<AppDayUsage>
)
