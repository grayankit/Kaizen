package me.ankitgrai.kaizen.data.usage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageRepository (context: Context){
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
}