package me.ankitgrai.kaizen.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun observeCustomCategories(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    suspend fun getCustomCategories(): List<CustomCategoryEntity>

    @Upsert
    suspend fun insertCategory(category: CustomCategoryEntity)

    @Query("DELETE FROM custom_categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    @Query("SELECT * FROM app_category_overrides")
    fun observeAppCategoryOverrides(): Flow<List<AppCategoryOverrideEntity>>

    @Query("SELECT * FROM app_category_overrides")
    suspend fun getAppCategoryOverrides(): List<AppCategoryOverrideEntity>

    @Upsert
    suspend fun setAppCategoryOverride(override: AppCategoryOverrideEntity)

    @Query("DELETE FROM app_category_overrides WHERE packageName = :packageName")
    suspend fun deleteAppCategoryOverride(packageName: String)

    @Query("DELETE FROM app_category_overrides WHERE categoryName = :categoryName")
    suspend fun deleteOverridesForCategory(categoryName: String)

    @Transaction
    suspend fun deleteCategoryAndClearOverrides(categoryName: String) {
        deleteCategory(categoryName)
        deleteOverridesForCategory(categoryName)
    }
}
