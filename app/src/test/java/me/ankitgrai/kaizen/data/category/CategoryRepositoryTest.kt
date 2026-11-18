package me.ankitgrai.kaizen.data.category

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.ankitgrai.kaizen.data.local.AppCategoryOverrideEntity
import me.ankitgrai.kaizen.data.local.CategoryDao
import me.ankitgrai.kaizen.data.local.CustomCategoryEntity
import me.ankitgrai.kaizen.domain.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCategoryDao : CategoryDao {
    private val categories = MutableStateFlow<List<CustomCategoryEntity>>(emptyList())
    private val overrides = MutableStateFlow<List<AppCategoryOverrideEntity>>(emptyList())

    override fun observeCustomCategories(): Flow<List<CustomCategoryEntity>> = categories

    override suspend fun getCustomCategories(): List<CustomCategoryEntity> = categories.value

    override suspend fun insertCategory(category: CustomCategoryEntity) {
        categories.value = (categories.value.filter { it.name != category.name } + category)
            .sortedBy { it.name }
    }

    override suspend fun deleteCategory(name: String) {
        categories.value = categories.value.filter { it.name != name }
    }

    override fun observeAppCategoryOverrides(): Flow<List<AppCategoryOverrideEntity>> = overrides

    override suspend fun getAppCategoryOverrides(): List<AppCategoryOverrideEntity> = overrides.value

    override suspend fun setAppCategoryOverride(override: AppCategoryOverrideEntity) {
        overrides.value = overrides.value.filter { it.packageName != override.packageName } + override
    }

    override suspend fun deleteAppCategoryOverride(packageName: String) {
        overrides.value = overrides.value.filter { it.packageName != packageName }
    }

    override suspend fun deleteOverridesForCategory(categoryName: String) {
        overrides.value = overrides.value.filter { it.categoryName != categoryName }
    }
}

class CategoryRepositoryTest {
    private lateinit var fakeDao: FakeCategoryDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() {
        fakeDao = FakeCategoryDao()
        repository = CategoryRepository(fakeDao)
    }

    @Test
    fun addCategoryValidatesInput() = runBlocking {
        // Blank name
        val blankResult = repository.addCategory("   ")
        assertTrue(blankResult.isFailure)

        // "All" is reserved
        val allResult = repository.addCategory("All")
        assertTrue(allResult.isFailure)

        // Default category cannot be added
        val defaultResult = repository.addCategory("Social")
        assertTrue(defaultResult.isFailure)

        // Valid new category
        val validResult = repository.addCategory("Study")
        assertTrue(validResult.isSuccess)

        // Duplicate custom category cannot be added
        val duplicateResult = repository.addCategory("study")
        assertTrue(duplicateResult.isFailure)
    }

    @Test
    fun observeAllCategoriesIncludesDefaultsAndCustom() = runBlocking {
        val initial = repository.observeAllCategories().first()
        assertEquals(AppCategory.defaultCategories, initial)

        repository.addCategory("Finance")
        val updated = repository.observeAllCategories().first()
        assertTrue("Finance" in updated)
        assertTrue(AppCategory.defaultCategories.all { it in updated })
    }

    @Test
    fun deleteCategoryRemovesCategoryAndOverrides() = runBlocking {
        repository.addCategory("Finance")
        repository.setAppCategory("com.bank.app", "Finance")

        val overridesBefore = repository.getAppCategoryOverrides()
        assertEquals("Finance", overridesBefore["com.bank.app"])

        repository.deleteCategory("Finance")
        val overridesAfter = repository.getAppCategoryOverrides()
        assertFalse("com.bank.app" in overridesAfter)

        val categoriesAfter = repository.observeAllCategories().first()
        assertFalse("Finance" in categoriesAfter)
    }

    @Test
    fun resolveCategoryUsesOverrideWhenAvailable() {
        val overrides = mapOf("com.instagram.android" to "Work")
        val resolved = repository.resolveCategory("com.instagram.android", "Instagram", overrides)
        assertEquals("Work", resolved)

        val unoverridden = repository.resolveCategory("com.instagram.android", "Instagram", emptyMap())
        assertEquals("Social", unoverridden)
    }
}
