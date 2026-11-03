package me.ankitgrai.kaizen.data.local

import androidx.room.Entity

@Entity(
    tableName = "hourly_device_usage",
    primaryKeys = ["date", "hour"]
)
data class HourlyDeviceUsageEntity(
    val date: String,
    val hour: Int,
    val totalUsageMillis: Long
)
