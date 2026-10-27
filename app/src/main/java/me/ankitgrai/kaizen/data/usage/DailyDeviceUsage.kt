package me.ankitgrai.kaizen.data.usage

import java.time.LocalDate

data class DailyDeviceUsage(
    val date: LocalDate,
    val totalUsageMillis: Long
)
