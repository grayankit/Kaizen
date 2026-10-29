package me.ankitgrai.kaizen.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ankitgrai.kaizen.data.local.DailyAppUsageEntity
import me.ankitgrai.kaizen.data.local.DailyDeviceUsageEntity
import me.ankitgrai.kaizen.data.local.KaizenDatabase

class UsageRepository (context: Context){
    private val appContext = context.applicationContext
    private val dataSource = UsageStateDataSource(
        context.applicationContext
    )
    private val historyDao = KaizenDatabase.get(appContext).usageHistoryDao()

    fun hasUsageAccess(): Boolean{
        return dataSource.hasUsageAccess()
    }

    fun openUsageAccessSettings(){
        dataSource.openUsageAccessSettings()
    }
    suspend fun getTodayUsage():List<AppUsageInfo>{
        return withContext(Dispatchers.IO){
            dataSource.getTodayUsage()
        }
    }
    suspend fun getInstalledApps():List<InstalledAppInfo>{
        return withContext(Dispatchers.IO){
            val packageManager = appContext.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply{
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            packageManager
                .queryIntentActivities(
                    launcherIntent,
                    PackageManager.MATCH_ALL
                )
                .mapNotNull { resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo
                        ?: return@mapNotNull null
                    val packageName = activityInfo.packageName
                    InstalledAppInfo(
                        packageName = packageName,
                        appName = resolveInfo.loadLabel(packageManager)
                            .toString()
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }

        }
    }
    suspend fun getAppUsageHistory(
        packageName: String,
        days: Int = 7
    ): List<DailyAppUsage> {
        return withContext(Dispatchers.IO) {
            refreshStoredHistory()
            val end = LocalDate.now()
            val start = end.minusDays((days - 1).toLong())
            val deviceDays = historyDao.deviceUsageBetween(
                start.toString(),
                end.toString()
            )
            val appDays = historyDao.appUsageBetween(
                packageName,
                start.toString(),
                end.toString()
            ).associateBy { it.date }
            deviceDays.map { device ->
                DailyAppUsage(
                    date = LocalDate.parse(device.date),
                    packageName = packageName,
                    totalTimeInForeground = appDays[device.date]?.totalUsageMillis ?: 0L
                )
            }
        }
    }
    suspend fun getDailyDeviceUsage(
        days: Int = 90
    ): List<DailyDeviceUsage> {
        return withContext(Dispatchers.IO) {
            refreshStoredHistory()
            val end = LocalDate.now()
            val start = end.minusDays((days - 1).toLong())
            historyDao.deviceUsageBetween(start.toString(), end.toString())
                .map {
                    DailyDeviceUsage(
                        date = LocalDate.parse(it.date),
                        totalUsageMillis = it.totalUsageMillis
                    )
                }
        }
    }

    suspend fun getTopAppsForDate(date: LocalDate): List<AppUsageInfo> {
        return withContext(Dispatchers.IO) {
            historyDao.appsOnDate(date.toString())
                .filter { it.totalUsageMillis > 0L }
                .map {
                    AppUsageInfo(
                        packageName = it.packageName,
                        appName = appLabel(it.packageName),
                        totalTimeInForeground = it.totalUsageMillis
                    )
                }
        }
    }

    private fun appLabel(packageName: String): String {
        return try {
            val info = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private suspend fun refreshStoredHistory() {
        if (!dataSource.hasUsageAccess()) return
        val observed = dataSource.observeRecentUsage(days = 14)
        if (observed.earliestObserved == null) return

        historyDao.upsertDeviceDays(
            observed.deviceTotals.map { (date, millis) ->
                DailyDeviceUsageEntity(
                    date = date.toString(),
                    totalUsageMillis = millis
                )
            }
        )
        historyDao.upsertAppDays(
            observed.appTotals.map { day ->
                DailyAppUsageEntity(
                    date = day.date.toString(),
                    packageName = day.packageName,
                    totalUsageMillis = day.totalUsageMillis
                )
            }
        )
    }
}