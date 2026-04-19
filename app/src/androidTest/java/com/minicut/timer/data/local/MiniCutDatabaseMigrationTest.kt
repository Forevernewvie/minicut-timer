package com.minicut.timer.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniCutDatabaseMigrationTest {

    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MiniCutDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate5To6_createsDailyConditionChecksTable() {
        helper.createDatabase(testDbName, 5).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mini_cut_plan` (
                    `id` INTEGER NOT NULL,
                    `startDateEpochDay` INTEGER NOT NULL,
                    `durationWeeks` INTEGER NOT NULL,
                    `endDateEpochDay` INTEGER NOT NULL,
                    `dailyTargetKcal` INTEGER NOT NULL,
                    `goalMode` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `calorie_entries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateEpochDay` INTEGER NOT NULL,
                    `calories` INTEGER NOT NULL,
                    `foodName` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `timeLabel` TEXT NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            testDbName,
            6,
            true,
            MiniCutDatabase.MIGRATION_5_6,
        ).apply {
            query("PRAGMA table_info('daily_condition_checks')").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("dateEpochDay"))
                assertTrue(columns.contains("bodyWeightKg"))
                assertTrue(columns.contains("proteinGrams"))
                assertTrue(columns.contains("resistanceSets"))
                assertTrue(columns.contains("updatedAtEpochMillis"))
            }
            close()
        }
    }

    @Test
    fun migrate4To6_addsGoalModeAndPreservesExistingPlanRow() {
        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mini_cut_plan` (
                    `id` INTEGER NOT NULL,
                    `startDateEpochDay` INTEGER NOT NULL,
                    `durationWeeks` INTEGER NOT NULL,
                    `endDateEpochDay` INTEGER NOT NULL,
                    `dailyTargetKcal` INTEGER NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `calorie_entries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateEpochDay` INTEGER NOT NULL,
                    `calories` INTEGER NOT NULL,
                    `foodName` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `timeLabel` TEXT NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO mini_cut_plan(id, startDateEpochDay, durationWeeks, endDateEpochDay, dailyTargetKcal, isActive)
                VALUES(1, 1000, 4, 1027, 1300, 1)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            testDbName,
            6,
            true,
            MiniCutDatabase.MIGRATION_4_5,
            MiniCutDatabase.MIGRATION_5_6,
        ).apply {
            query("SELECT goalMode, dailyTargetKcal FROM mini_cut_plan WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                val goalMode = cursor.getString(cursor.getColumnIndexOrThrow("goalMode"))
                val dailyTarget = cursor.getInt(cursor.getColumnIndexOrThrow("dailyTargetKcal"))
                assertEquals("MassReset", goalMode)
                assertEquals(1300, dailyTarget)
            }
            close()
        }
    }
}
