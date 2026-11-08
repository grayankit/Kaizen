package me.ankitgrai.kaizen.data.reel

import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ankitgrai.kaizen.data.local.KaizenDatabase
import me.ankitgrai.kaizen.data.local.ReelSessionEntity

class ReelRepository(context: Context) {
    private val dao = KaizenDatabase.get(context).reelDao()
    private val counter = ReelCounter(context)

    suspend fun refresh(enabledApps: Set<String>) = withContext(Dispatchers.IO) {
        if (enabledApps.isEmpty()) return@withContext
        val today = LocalDate.now().toString()
        val counts = counter.countToday(enabledApps)
        dao.upsert(counts.map { (pkg, count) ->
            ReelSessionEntity(date = today, packageName = pkg, resumeCount = count)
        })
    }

    suspend fun todayTotal(enabledApps: Set<String>): Int = withContext(Dispatchers.IO) {
        dao.forDate(LocalDate.now().toString())
            .filter { it.packageName in enabledApps }
            .sumOf { it.resumeCount }
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
                        count = rows.sumOf { it.resumeCount },
                    )
                }
                .sortedBy { it.date }
        }
}
