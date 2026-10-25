package me.ankitgrai.kaizen.data.usage

import java.time.LocalDate

data class DailyAppUsage (
    val date: LocalDate,
    val packageName: String,
    val totalTimeInForeground: Long
)