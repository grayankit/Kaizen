package me.ankitgrai.kaizen.data.local

import androidx.room.Entity

@Entity(tableName = "reel_sessions", primaryKeys = ["date", "packageName"])
data class ReelSessionEntity(
    val date: String,
    val packageName: String,
    val resumeCount: Int,
)
