package com.minicut.timer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minicut.timer.data.local.dao.CalorieEntryDao
import com.minicut.timer.data.local.dao.DailyConditionCheckDao
import com.minicut.timer.data.local.dao.MiniCutPlanDao
import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.DailyConditionCheckEntity
import com.minicut.timer.data.local.entity.MiniCutPlanEntity

@Database(
    entities = [MiniCutPlanEntity::class, CalorieEntryEntity::class, DailyConditionCheckEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class MiniCutDatabase : RoomDatabase() {
    abstract fun planDao(): MiniCutPlanDao
    abstract fun calorieEntryDao(): CalorieEntryDao
    abstract fun dailyConditionCheckDao(): DailyConditionCheckDao

    companion object {
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE calorie_entries ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE mini_cut_plan ADD COLUMN goalMode TEXT NOT NULL DEFAULT 'MassReset'",
                    )
                }
            }

        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS daily_condition_checks (
                            dateEpochDay INTEGER NOT NULL,
                            bodyWeightKg REAL,
                            proteinGrams INTEGER,
                            resistanceSets INTEGER,
                            updatedAtEpochMillis INTEGER NOT NULL,
                            PRIMARY KEY(dateEpochDay)
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE mini_cut_plan ADD COLUMN activityLevel TEXT NOT NULL DEFAULT 'Moderate'",
                    )
                    db.execSQL(
                        "ALTER TABLE mini_cut_plan ADD COLUMN estimatedMaintenanceKcal INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "ALTER TABLE daily_condition_checks ADD COLUMN sleepHours REAL",
                    )
                    db.execSQL(
                        "ALTER TABLE daily_condition_checks ADD COLUMN fatigueScore INTEGER",
                    )
                    db.execSQL(
                        "ALTER TABLE daily_condition_checks ADD COLUMN hungerScore INTEGER",
                    )
                    db.execSQL(
                        "ALTER TABLE daily_condition_checks ADD COLUMN moodScore INTEGER",
                    )
                    db.execSQL(
                        "ALTER TABLE daily_condition_checks ADD COLUMN workoutPerformanceScore INTEGER",
                    )
                }
            }

        val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE daily_condition_checks ADD COLUMN mainLiftKg REAL",
                    )
                }
            }
    }
}
