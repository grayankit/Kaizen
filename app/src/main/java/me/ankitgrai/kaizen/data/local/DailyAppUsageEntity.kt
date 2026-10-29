package me.ankitgrai.kaizen.data.local

import androidx.room.Entity

@Entity(
    tableName = "daily_app_usage",
    primaryKeys = ["date", "packageName"]
)
data class DailyAppUsageEntity(
    val date: String,
    val packageName: String,
    val totalUsageMillis: Long
)
