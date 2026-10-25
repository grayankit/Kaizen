package me.ankitgrai.kaizen.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageRepository (context: Context){
    private val appContext = context.applicationContext
    private val dataSource = UsageStateDataSource(
        context.applicationContext
    )

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
            dataSource.getAppUsageHistory(packageName, days)
        }
    }
}