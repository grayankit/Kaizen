package me.ankitgrai.kaizen.data.usage

import java.time.LocalDate
import me.ankitgrai.kaizen.data.local.DailyAppUsageEntity
import me.ankitgrai.kaizen.data.local.DailyDeviceUsageEntity
import me.ankitgrai.kaizen.data.local.HourlyDeviceUsageEntity

internal fun DailyDeviceUsageEntity.toDailyDeviceUsage() = DailyDeviceUsage(
    date = LocalDate.parse(date),
    totalUsageMillis = totalUsageMillis,
)

internal fun HourlyDeviceUsageEntity.toHourlyDeviceUsage() = HourlyDeviceUsage(
    date = LocalDate.parse(date),
    hour = hour,
    totalUsageMillis = totalUsageMillis,
)

internal fun DailyAppUsageEntity.toDailyAppUsage() = DailyAppUsage(
    date = LocalDate.parse(date),
    packageName = packageName,
    totalTimeInForeground = totalUsageMillis,
)
