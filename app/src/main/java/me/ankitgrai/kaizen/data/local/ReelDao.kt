package me.ankitgrai.kaizen.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ReelDao {
    @Upsert
    suspend fun upsert(rows: List<ReelSessionEntity>)

    @Query(
        """
        SELECT swipeCount FROM reel_sessions
        WHERE date = :date AND packageName = :packageName
        """
    )
    suspend fun swipeCount(date: String, packageName: String): Int?

    @Query("SELECT * FROM reel_sessions WHERE date = :date")
    suspend fun forDate(date: String): List<ReelSessionEntity>

    @Query(
        """
        SELECT * FROM reel_sessions
        WHERE date >= :start AND date <= :end
        ORDER BY date
        """
    )
    suspend fun forRange(start: String, end: String): List<ReelSessionEntity>
}
