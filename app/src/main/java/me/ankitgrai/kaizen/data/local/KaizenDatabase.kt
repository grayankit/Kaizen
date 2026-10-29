package me.ankitgrai.kaizen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyDeviceUsageEntity::class,
        DailyAppUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KaizenDatabase : RoomDatabase() {
    abstract fun usageHistoryDao(): UsageHistoryDao

    companion object {
        @Volatile
        private var instance: KaizenDatabase? = null

        fun get(context: Context): KaizenDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KaizenDatabase::class.java,
                    "kaizen.db"
                ).build().also { instance = it }
            }
        }
    }
}
