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
