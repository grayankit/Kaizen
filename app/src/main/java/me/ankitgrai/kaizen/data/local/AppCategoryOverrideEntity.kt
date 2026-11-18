package me.ankitgrai.kaizen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category_overrides")
data class AppCategoryOverrideEntity(
    @PrimaryKey val packageName: String,
    val categoryName: String
)
