package me.ankitgrai.kaizen.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UsageHistoryDao {
    @Query(
        """
        SELECT * FROM daily_device_usage
        WHERE date >= :start AND date <= :end
        ORDER BY date
        """
    )
    suspend fun deviceUsageBetween(
        start: String,
        end: String
    ): List<DailyDeviceUsageEntity>

    @Upsert
    suspend fun upsertDeviceDays(days: List<DailyDeviceUsageEntity>)

    @Query(
        """
        SELECT * FROM hourly_device_usage
        WHERE date = :date
        ORDER BY hour
        """
    )
    suspend fun hoursOnDate(date: String): List<HourlyDeviceUsageEntity>

    @Upsert
    suspend fun upsertDeviceHours(hours: List<HourlyDeviceUsageEntity>)

    @Query(
        """
        SELECT packageName, SUM(totalUsageMillis) AS totalUsageMillis
        FROM daily_app_usage
        WHERE date >= :start AND date <= :end
        GROUP BY packageName
        ORDER BY totalUsageMillis DESC
        """
    )
    suspend fun appUsageTotalsBetween(
        start: String,
        end: String
    ): List<AppUsageTotal>

    @Query(
        """
        SELECT * FROM daily_app_usage
        WHERE packageName = :packageName AND date >= :start AND date <= :end
        ORDER BY date
        """
    )
    suspend fun appUsageBetween(
        packageName: String,
        start: String,
        end: String
    ): List<DailyAppUsageEntity>

    @Upsert
    suspend fun upsertAppDays(days: List<DailyAppUsageEntity>)
}

data class AppUsageTotal(
    val packageName: String,
    val totalUsageMillis: Long
)
