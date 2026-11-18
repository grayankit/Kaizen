package me.ankitgrai.kaizen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyDeviceUsageEntity::class,
        DailyAppUsageEntity::class,
        HourlyDeviceUsageEntity::class,
        ReelSessionEntity::class,
        CustomCategoryEntity::class,
        AppCategoryOverrideEntity::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class KaizenDatabase : RoomDatabase() {
    abstract fun usageHistoryDao(): UsageHistoryDao
    abstract fun reelDao(): ReelDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: KaizenDatabase? = null

        fun get(context: Context): KaizenDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KaizenDatabase::class.java,
                    "kaizen.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `hourly_device_usage` (
                `date` TEXT NOT NULL,
                `hour` INTEGER NOT NULL,
                `totalUsageMillis` INTEGER NOT NULL,
                PRIMARY KEY(`date`, `hour`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reel_sessions` (
                `date` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `resumeCount` INTEGER NOT NULL,
                PRIMARY KEY(`date`, `packageName`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `reel_sessions`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reel_sessions` (
                `date` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `swipeCount` INTEGER NOT NULL,
                PRIMARY KEY(`date`, `packageName`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_categories` (
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`name`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `app_category_overrides` (
                `packageName` TEXT NOT NULL,
                `categoryName` TEXT NOT NULL,
                PRIMARY KEY(`packageName`)
            )
            """.trimIndent()
        )
    }
}
