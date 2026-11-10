package me.ankitgrai.kaizen.data.reel

import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.ankitgrai.kaizen.data.local.KaizenDatabase
import me.ankitgrai.kaizen.data.local.ReelSessionEntity

class ReelRepository(context: Context) {
    private val dao = KaizenDatabase.get(context).reelDao()
    private val writes = Mutex()

    suspend fun recordSwipe(packageName: String) = withContext(Dispatchers.IO) {
        val date = LocalDate.now().toString()
        writes.withLock {
            val next = (dao.swipeCount(date, packageName) ?: 0) + 1
            dao.upsert(listOf(ReelSessionEntity(date, packageName, next)))
        }
    }

    suspend fun todayTotal(enabledApps: Set<String>): Int = withContext(Dispatchers.IO) {
        dao.forDate(LocalDate.now().toString())
            .filter { it.packageName in enabledApps }
            .sumOf { it.swipeCount }
    }

    suspend fun last7Days(enabledApps: Set<String>): List<DailyReelCount> =
        withContext(Dispatchers.IO) {
            val end = LocalDate.now()
            val start = end.minusDays(6)
            dao.forRange(start.toString(), end.toString())
                .filter { it.packageName in enabledApps }
                .groupBy { it.date }
                .map { (date, rows) ->
                    DailyReelCount(
                        date = LocalDate.parse(date),
                        count = rows.sumOf { it.swipeCount },
                    )
                }
                .sortedBy { it.date }
        }
}
