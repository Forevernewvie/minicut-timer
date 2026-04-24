package com.minicut.timer.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniCutDatabaseMigrationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migrate3To4_addsFavoriteFlagToCalorieEntries() =
        openDatabase("migration-3-4.db", version = 3) { createVersion3Tables() }.use { managed ->
            managed.db.applyMigrations(MiniCutDatabase.MIGRATION_3_4)

            managed.db.assertTableHasColumns("calorie_entries", "isFavorite")
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate4To5_addsGoalModeToPlan() =
        openDatabase("migration-4-5.db", version = 4) { createVersion4Tables() }.use { managed ->
            managed.db.applyMigrations(MiniCutDatabase.MIGRATION_4_5)

            managed.db.assertTableHasColumns("mini_cut_plan", "goalMode")
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate5To6_createsDailyConditionChecksTable() =
        openDatabase("migration-5-6.db", version = 5) { createVersion5Tables() }.use { managed ->
            managed.db.applyMigrations(MiniCutDatabase.MIGRATION_5_6)

            managed.db.assertTableHasColumns(
                "daily_condition_checks",
                "dateEpochDay",
                "bodyWeightKg",
                "proteinGrams",
                "resistanceSets",
                "updatedAtEpochMillis",
            )
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate6To7_addsGuardrailAndRecoveryColumns() =
        openDatabase("migration-6-7.db", version = 6) {
            createVersion6Tables()
            execSQL(
                """
                INSERT INTO mini_cut_plan(id, startDateEpochDay, durationWeeks, endDateEpochDay, dailyTargetKcal, goalMode, isActive)
                VALUES(1, 1000, 4, 1027, 1300, 'MassReset', 1)
                """.trimIndent(),
            )
        }.use { managed ->
            managed.db.applyMigrations(MiniCutDatabase.MIGRATION_6_7)

            managed.db.assertTableHasColumns("mini_cut_plan", "activityLevel", "estimatedMaintenanceKcal")
            managed.db.assertTableHasColumns(
                "daily_condition_checks",
                "sleepHours",
                "fatigueScore",
                "hungerScore",
                "moodScore",
                "workoutPerformanceScore",
            )
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate7To8_addsMainLiftKgColumn() =
        openDatabase("migration-7-8.db", version = 7) { createVersion7Tables() }.use { managed ->
            managed.db.applyMigrations(MiniCutDatabase.MIGRATION_7_8)

            managed.db.assertTableHasColumns("daily_condition_checks", "mainLiftKg")
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate8To9_addsRelapseColumns() =
        openDatabase("migration-8-9.db", version = 8) { createVersion8Tables() }.use { managed ->
            managed.db.applyMigrations(MiniCutDatabase.MIGRATION_8_9)

            managed.db.assertTableHasColumns("daily_condition_checks", "relapseTrigger", "copingAction")
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate4To9_preservesPlanRowAndAppliesDefaultsForNewColumns() =
        openDatabase("migration-4-9.db", version = 4) {
            createVersion4Tables()
            execSQL(
                """
                INSERT INTO mini_cut_plan(id, startDateEpochDay, durationWeeks, endDateEpochDay, dailyTargetKcal, isActive)
                VALUES(1, 1000, 4, 1027, 1400, 1)
                """.trimIndent(),
            )
        }.use { managed ->
            managed.db.applyMigrations(
                MiniCutDatabase.MIGRATION_4_5,
                MiniCutDatabase.MIGRATION_5_6,
                MiniCutDatabase.MIGRATION_6_7,
                MiniCutDatabase.MIGRATION_7_8,
                MiniCutDatabase.MIGRATION_8_9,
            )

            managed.db.query("SELECT goalMode, activityLevel, estimatedMaintenanceKcal, dailyTargetKcal FROM mini_cut_plan WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("MassReset", cursor.getString(0))
                assertEquals("Moderate", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals(1400, cursor.getInt(3))
            }
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    @Test
    fun migrate3To9_preservesSupportedLegacyDataPath() =
        openDatabase("migration-3-9.db", version = 3) {
            createVersion3Tables()
            execSQL(
                """
                INSERT INTO mini_cut_plan(id, startDateEpochDay, durationWeeks, endDateEpochDay, dailyTargetKcal, isActive)
                VALUES(1, 2000, 4, 2027, 1400, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO calorie_entries(id, dateEpochDay, calories, foodName, note, timeLabel, createdAtEpochMillis)
                VALUES(10, 2000, 450, '닭가슴살', '점심', '12:00', 1710000000000)
                """.trimIndent(),
            )
        }.use { managed ->
            managed.db.applyMigrations(
                MiniCutDatabase.MIGRATION_3_4,
                MiniCutDatabase.MIGRATION_4_5,
                MiniCutDatabase.MIGRATION_5_6,
                MiniCutDatabase.MIGRATION_6_7,
                MiniCutDatabase.MIGRATION_7_8,
                MiniCutDatabase.MIGRATION_8_9,
            )

            managed.db.query("SELECT goalMode, activityLevel, estimatedMaintenanceKcal FROM mini_cut_plan WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("MassReset", cursor.getString(0))
                assertEquals("Moderate", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
            }
            managed.db.query("SELECT isFavorite, foodName, calories FROM calorie_entries WHERE id = 10").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals("닭가슴살", cursor.getString(1))
                assertEquals(450, cursor.getInt(2))
            }
            managed.db.assertIndexExists("index_calorie_entries_dateEpochDay")
            managed.validateWithRoom()
        }

    private fun openDatabase(
        name: String,
        version: Int,
        onCreate: SupportSQLiteDatabase.() -> Unit,
    ): ManagedDatabase {
        context.deleteDatabase(name)
        val callback =
            object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    onCreate.invoke(db)
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            }
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(callback)
                .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        return ManagedDatabase(name = name, helper = helper, db = db, context = context)
    }

    private fun SupportSQLiteDatabase.applyMigrations(vararg migrations: Migration) {
        migrations.forEach { migration ->
            migration.migrate(this)
            version = migration.endVersion
        }
    }

    private fun SupportSQLiteDatabase.assertTableHasColumns(
        tableName: String,
        vararg expectedColumns: String,
    ) {
        query("PRAGMA table_info('$tableName')").use { cursor ->
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
            expectedColumns.forEach { column ->
                assertTrue("Expected column '$column' in table '$tableName'", columns.contains(column))
            }
        }
    }

    private fun SupportSQLiteDatabase.assertIndexExists(indexName: String) {
        query("PRAGMA index_list('calorie_entries')").use { cursor ->
            val indices = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                indices += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
            assertTrue("Expected index '$indexName' on calorie_entries", indices.contains(indexName))
        }
    }

    private fun SupportSQLiteDatabase.createVersion3Tables() {
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
                `createdAtEpochMillis` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_calorie_entries_dateEpochDay` ON `calorie_entries` (`dateEpochDay`)",
        )
    }

    private fun SupportSQLiteDatabase.createVersion4Tables() {
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
            "CREATE INDEX IF NOT EXISTS `index_calorie_entries_dateEpochDay` ON `calorie_entries` (`dateEpochDay`)",
        )
    }

    private fun SupportSQLiteDatabase.createVersion5Tables() {
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
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_calorie_entries_dateEpochDay` ON `calorie_entries` (`dateEpochDay`)",
        )
    }

    private fun SupportSQLiteDatabase.createVersion6Tables() {
        createVersion5Tables()
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_condition_checks` (
                `dateEpochDay` INTEGER NOT NULL,
                `bodyWeightKg` REAL,
                `proteinGrams` INTEGER,
                `resistanceSets` INTEGER,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`dateEpochDay`)
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.createVersion7Tables() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `mini_cut_plan` (
                `id` INTEGER NOT NULL,
                `startDateEpochDay` INTEGER NOT NULL,
                `durationWeeks` INTEGER NOT NULL,
                `endDateEpochDay` INTEGER NOT NULL,
                `dailyTargetKcal` INTEGER NOT NULL,
                `goalMode` TEXT NOT NULL,
                `activityLevel` TEXT NOT NULL,
                `estimatedMaintenanceKcal` INTEGER NOT NULL,
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
            "CREATE INDEX IF NOT EXISTS `index_calorie_entries_dateEpochDay` ON `calorie_entries` (`dateEpochDay`)",
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_condition_checks` (
                `dateEpochDay` INTEGER NOT NULL,
                `bodyWeightKg` REAL,
                `proteinGrams` INTEGER,
                `resistanceSets` INTEGER,
                `sleepHours` REAL,
                `fatigueScore` INTEGER,
                `hungerScore` INTEGER,
                `moodScore` INTEGER,
                `workoutPerformanceScore` INTEGER,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`dateEpochDay`)
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.createVersion8Tables() {
        createVersion7Tables()
        execSQL("ALTER TABLE daily_condition_checks ADD COLUMN mainLiftKg REAL")
    }

    private data class ManagedDatabase(
        val name: String,
        val helper: SupportSQLiteOpenHelper,
        val db: SupportSQLiteDatabase,
        val context: Context,
    ) : AutoCloseable {
        fun validateWithRoom() {
            helper.close()
            val roomDb =
                Room.databaseBuilder(context, MiniCutDatabase::class.java, name)
                    .addMigrations(
                        MiniCutDatabase.MIGRATION_3_4,
                        MiniCutDatabase.MIGRATION_4_5,
                        MiniCutDatabase.MIGRATION_5_6,
                        MiniCutDatabase.MIGRATION_6_7,
                        MiniCutDatabase.MIGRATION_7_8,
                        MiniCutDatabase.MIGRATION_8_9,
                    ).build()
            roomDb.openHelper.writableDatabase.close()
            roomDb.close()
        }

        override fun close() {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
