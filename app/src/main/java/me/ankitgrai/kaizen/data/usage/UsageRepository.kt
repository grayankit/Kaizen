package me.ankitgrai.kaizen.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val refreshMutex = Mutex()
    private val labelCache = mutableMapOf<String, String>()
    private var installedCache: List<InstalledAppInfo>? = null
    private var cachedToday: List<AppUsageInfo> = emptyList()
    private var snapshotAt = 0L
    private var snapshotReady = false

    fun hasUsageAccess(): Boolean{
        return dataSource.hasUsageAccess()
    }

    fun openUsageAccessSettings(){
        dataSource.openUsageAccessSettings()
    }
    suspend fun getTodayUsage(): List<AppUsageInfo> = refreshUsage()

    suspend fun getInstalledApps(): List<InstalledAppInfo> {
        return withContext(Dispatchers.IO) {
            installedCache?.let { return@withContext it }
            val packageManager = appContext.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
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
                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    labelCache.putIfAbsent(packageName, appName)
                    InstalledAppInfo(
                        packageName = packageName,
                        appName = appName
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
                .also { installedCache = it }
        }
    }
    suspend fun getAppUsageHistory(
        packageName: String,
        days: Int = 7
    ): List<DailyAppUsage> {
        return withContext(Dispatchers.IO) {
            refreshUsage()
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
        days: Int = 90,
        force: Boolean = false
    ): List<DailyDeviceUsage> {
        return withContext(Dispatchers.IO) {
            refreshUsage(force)
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
        labelCache[packageName]?.let { return it }
        val label = try {
            val info = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
        labelCache[packageName] = label
        return label
    }

    /**
     * One event walk for every screen. Callers that arrive while this is running
     * wait for it, and a finished snapshot is reused for a minute.
     */
    suspend fun refreshUsage(force: Boolean = false): List<AppUsageInfo> {
        return withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                val now = System.currentTimeMillis()
                if (!force && snapshotReady && now - snapshotAt < SNAPSHOT_TTL_MS) {
                    return@withLock cachedToday
                }
                if (!dataSource.hasUsageAccess()) {
                    return@withLock cachedToday
                }
                val observed = dataSource.observeRecentUsage(days = 14)
                if (observed.earliestObserved == null) {
                    return@withLock cachedToday
                }
                val today = LocalDate.now()
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
                cachedToday = observed.appTotals
                    .asSequence()
                    .filter {
                        it.date == today &&
                            it.totalUsageMillis > 0L &&
                            it.packageName != appContext.packageName
                    }
                    .map {
                        AppUsageInfo(
                            packageName = it.packageName,
                            appName = appLabel(it.packageName),
                            totalTimeInForeground = it.totalUsageMillis
                        )
                    }
                    .sortedByDescending { it.totalTimeInForeground }
                    .toList()
                snapshotAt = now
                snapshotReady = true
                cachedToday
            }
        }
    }

    private companion object {
        const val SNAPSHOT_TTL_MS = 60_000L
    }
}