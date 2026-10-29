package me.ankitgrai.kaizen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_device_usage")
data class DailyDeviceUsageEntity(
    @PrimaryKey val date: String,
    val totalUsageMillis: Long
)
