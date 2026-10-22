package me.ankitgrai.kaizen.data.usage

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long
)