package me.ankitgrai.kaizen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
