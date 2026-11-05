package me.ankitgrai.kaizen.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.usage.UsageRepository

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var lastPublishAt = 0L

    fun publish(context: Context) {
        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPublishAt < PUBLISH_DEBOUNCE_MS) return
        lastPublishAt = now
        ensureScheduled(appContext)
        scope.launch { update(appContext) }
    }

    suspend fun update(context: Context) {
        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) {
            cancelRefresh(appContext)
            return
        }
        ScreenTimeWidget().updateAll(appContext)
    }

    fun refreshSoon(context: Context) {
        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) return
        ensureScheduled(appContext)
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            ONCE_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelRefresh(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(ONCE_WORK)
        workManager.cancelUniqueWork(PERIODIC_WORK)
    }

    private fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            30,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ScreenTimeWidgetReceiver::class.java)
        return manager.getAppWidgetIds(component).isNotEmpty()
    }

    private const val ONCE_WORK = "kaizen.widget.refresh.once"
    private const val PERIODIC_WORK = "kaizen.widget.refresh.periodic"
    private const val PUBLISH_DEBOUNCE_MS = 2_000L
}

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!widgetStillPlaced(applicationContext)) {
            WidgetUpdater.cancelRefresh(applicationContext)
            return Result.success()
        }
        val repository = UsageRepository(applicationContext)
        if (repository.hasUsageAccess()) {
            runCatching {
                repository.refreshUsage(force = true, publishWidget = false)
            }
        }
        WidgetUpdater.update(applicationContext)
        return Result.success()
    }
}

private fun widgetStillPlaced(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    val component = ComponentName(context, ScreenTimeWidgetReceiver::class.java)
    return manager.getAppWidgetIds(component).isNotEmpty()
}
