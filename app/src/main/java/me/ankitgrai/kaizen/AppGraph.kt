package me.ankitgrai.kaizen

import android.content.Context
import me.ankitgrai.kaizen.data.category.CategoryRepository
import me.ankitgrai.kaizen.data.usage.UsageRepository
import me.ankitgrai.kaizen.widget.WidgetUpdater

/**
 * One usage repository for the process. The in-memory snapshot and refresh
 * lock have to be shared by the screens and the widgets, or each caller
 * walks UsageStats on its own.
 */
object AppGraph {
    @Volatile
    private var usageRepository: UsageRepository? = null

    @Volatile
    private var categoryRepository: CategoryRepository? = null

    fun usageRepository(context: Context): UsageRepository {
        usageRepository?.let { return it }
        return synchronized(this) {
            usageRepository ?: UsageRepository(
                context.applicationContext,
                onUsageChanged = WidgetUpdater::publish,
            ).also { usageRepository = it }
        }
    }

    fun categoryRepository(context: Context): CategoryRepository {
        categoryRepository?.let { return it }
        return synchronized(this) {
            categoryRepository ?: CategoryRepository(
                context.applicationContext
            ).also { categoryRepository = it }
        }
    }
}
