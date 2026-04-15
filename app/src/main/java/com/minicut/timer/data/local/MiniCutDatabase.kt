package com.minicut.timer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minicut.timer.data.local.dao.CalorieEntryDao
import com.minicut.timer.data.local.dao.MiniCutPlanDao
import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.MiniCutPlanEntity

@Database(
    entities = [MiniCutPlanEntity::class, CalorieEntryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class MiniCutDatabase : RoomDatabase() {
    abstract fun planDao(): MiniCutPlanDao
    abstract fun calorieEntryDao(): CalorieEntryDao

    companion object {
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE calorie_entries ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }
    }
}
