package me.ankitgrai.kaizen.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.usage.UsageRepository
import kotlin.time.Duration.Companion.milliseconds

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var lastPublishAt = 0L
    private var reelPublish: Job? = null

    fun publish(context: Context) {
        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPublishAt < PUBLISH_DEBOUNCE_MS) return
        lastPublishAt = now
        ensureScheduled(appContext)
        scope.launch { update(appContext) }
    }

    fun publishReel(context: Context) {
        val appContext = context.applicationContext
        if (!hasReelWidget(appContext)) return
        reelPublish?.cancel()
        reelPublish = scope.launch {
            delay(REEL_PUBLISH_DELAY_MS.milliseconds)
            runCatching {
                updatePlaced(
                    appContext,
                    ReelCountWidget(),
                    ReelCountWidgetReceiver::class.java,
                )
            }
        }
    }

    suspend fun update(context: Context) {
        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) {
            cancelRefresh(appContext)
            return
        }
        updatePlaced(appContext, ScreenTimeWidget(), ScreenTimeWidgetReceiver::class.java)
        updatePlaced(appContext, ReelCountWidget(), ReelCountWidgetReceiver::class.java)
    }

    // Update by the receiver's widget ids, not GlanceAppWidget.updateAll.
    // updateAll looks widgets up by class name, which painted both widgets
    // when R8 merged the two GlanceAppWidget subclasses.
    private suspend fun updatePlaced(
        context: Context,
        widget: GlanceAppWidget,
        receiver: Class<*>,
    ) {
        val ids = widgetIds(context, receiver)
        if (ids.isEmpty()) return
        val manager = GlanceAppWidgetManager(context)
        ids.forEach { id ->
            widget.update(context, manager.getGlanceIdBy(id))
        }
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

    fun cancelRefreshIfUnused(context: Context) {
        if (!hasWidgets(context.applicationContext)) {
            cancelRefresh(context)
        }
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

    internal fun hasWidgets(context: Context): Boolean {
        return hasScreenTimeWidget(context) || hasReelWidget(context)
    }

    private fun hasScreenTimeWidget(context: Context): Boolean {
        return widgetIds(context, ScreenTimeWidgetReceiver::class.java).isNotEmpty()
    }

    private fun hasReelWidget(context: Context): Boolean {
        return widgetIds(context, ReelCountWidgetReceiver::class.java).isNotEmpty()
    }

    private fun widgetIds(context: Context, receiver: Class<*>): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, receiver))
    }

    private const val ONCE_WORK = "kaizen.widget.refresh.once"
    private const val PERIODIC_WORK = "kaizen.widget.refresh.periodic"
    private const val PUBLISH_DEBOUNCE_MS = 2_000L
    private const val REEL_PUBLISH_DELAY_MS = 300L
}

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!WidgetUpdater.hasWidgets(applicationContext)) {
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
