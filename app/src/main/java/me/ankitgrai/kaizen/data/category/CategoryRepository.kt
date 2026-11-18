package me.ankitgrai.kaizen.data.category

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.ankitgrai.kaizen.data.local.AppCategoryOverrideEntity
import me.ankitgrai.kaizen.data.local.CategoryDao
import me.ankitgrai.kaizen.data.local.CustomCategoryEntity
import me.ankitgrai.kaizen.data.local.KaizenDatabase
import me.ankitgrai.kaizen.domain.AppCategory
import me.ankitgrai.kaizen.domain.categorizeApp

class CategoryRepository(
    private val categoryDao: CategoryDao,
) {
    constructor(context: Context) : this(
        KaizenDatabase.get(context).categoryDao()
    )

    fun observeCustomCategories(): Flow<List<CustomCategoryEntity>> =
        categoryDao.observeCustomCategories()

    /**
     * Flow of all available categories: Default categories (Social, Video, etc.)
     * plus user-created custom categories.
     */
    fun observeAllCategories(): Flow<List<String>> =
        categoryDao.observeCustomCategories().map { customList ->
            val customNames = customList.map { it.name }
            AppCategory.defaultCategories + customNames
        }

    /**
     * Flow mapping packageName -> categoryName
     */
    fun observeAppCategoryOverrides(): Flow<Map<String, String>> =
        categoryDao.observeAppCategoryOverrides().map { list ->
            list.associate { it.packageName to it.categoryName }
        }

    suspend fun getAppCategoryOverrides(): Map<String, String> =
        categoryDao.getAppCategoryOverrides().associate { it.packageName to it.categoryName }

    suspend fun addCategory(name: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        if (trimmed.equals(AppCategory.All.label, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("'All' is a reserved category name"))
        }
        val isDefault = AppCategory.defaultCategories.any { it.equals(trimmed, ignoreCase = true) }
        if (isDefault) {
            return Result.failure(IllegalArgumentException("'$trimmed' already exists as a default category"))
        }
        val existingCustom = categoryDao.getCustomCategories()
        if (existingCustom.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Category '$trimmed' already exists"))
        }
        categoryDao.insertCategory(CustomCategoryEntity(name = trimmed))
        return Result.success(Unit)
    }

    suspend fun deleteCategory(name: String) {
        categoryDao.deleteCategoryAndClearOverrides(name)
    }

    suspend fun setAppCategory(packageName: String, categoryName: String) {
        categoryDao.setAppCategoryOverride(
            AppCategoryOverrideEntity(
                packageName = packageName,
                categoryName = categoryName
            )
        )
    }

    suspend fun resetAppCategory(packageName: String) {
        categoryDao.deleteAppCategoryOverride(packageName)
    }

    fun resolveCategory(
        packageName: String,
        appName: String,
        overrides: Map<String, String>
    ): String {
        return overrides[packageName] ?: categorizeApp(packageName, appName)
    }
}
