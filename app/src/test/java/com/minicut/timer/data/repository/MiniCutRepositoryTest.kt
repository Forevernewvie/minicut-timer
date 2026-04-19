package com.minicut.timer.data.repository

import com.minicut.timer.data.local.dao.CalorieEntryDao
import com.minicut.timer.data.local.dao.MiniCutPlanDao
import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.MiniCutPlanEntity
import com.minicut.timer.data.local.query.DailyCalorieSummaryRow
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.EntryQuickPreset
import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.testing.FakeCalorieEntryDao
import com.minicut.timer.testing.FakeDailyConditionCheckDao
import com.minicut.timer.testing.FakeMiniCutPlanDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MiniCutRepositoryTest {

    @Test
    fun savePlan_persistsCalculatedInclusiveEndDate() = runTest {
        val planDao = FakeMiniCutPlanDao()
        val repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), FakeDailyConditionCheckDao())

        val startDate = LocalDate.of(2026, 4, 10)
        repository.savePlan(
            startDate = startDate,
            durationWeeks = 4,
            dailyTargetKcal = 1300,
            goalMode = MiniCutGoalMode.EventReady,
            activityLevel = ActivityLevel.High,
            estimatedMaintenanceKcal = 2500,
        )

        val saved = planDao.lastUpsert
        assertNotNull(saved)
        assertEquals(startDate.toEpochDay(), saved?.startDateEpochDay)
        assertEquals(4, saved?.durationWeeks)
        assertEquals(LocalDate.of(2026, 5, 7).toEpochDay(), saved?.endDateEpochDay)
        assertEquals(1300, saved?.dailyTargetKcal)
        assertEquals(MiniCutGoalMode.EventReady.name, saved?.goalMode)
        assertEquals(ActivityLevel.High.name, saved?.activityLevel)
        assertEquals(2500, saved?.estimatedMaintenanceKcal)
        assertEquals(true, saved?.isActive)
    }

    @Test
    fun savePlan_rejectsTargetOutsideRecommendedRange() = runTest {
        val planDao = FakeMiniCutPlanDao()
        val repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), FakeDailyConditionCheckDao())

        try {
            repository.savePlan(
                startDate = LocalDate.of(2026, 4, 10),
                durationWeeks = 4,
                dailyTargetKcal = 900,
            )
            fail("Expected IllegalArgumentException for invalid daily target")
        } catch (_: IllegalArgumentException) {
            // expected
        }

        assertNull(planDao.lastUpsert)
    }

    @Test
    fun addEntry_trimsUserInputBeforePersisting() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao())
        val date = LocalDate.of(2026, 4, 10)

        repository.addEntry(
            date = date,
            calories = 420,
            foodName = " 닭가슴살 ",
            note = " 점심 기록 ",
            timeLabel = " 12:30 ",
        )

        val inserted = calorieDao.lastInserted
        assertNotNull(inserted)
        assertEquals(date.toEpochDay(), inserted?.dateEpochDay)
        assertEquals(420, inserted?.calories)
        assertEquals("닭가슴살", inserted?.foodName)
        assertEquals("점심 기록", inserted?.note)
        assertEquals("12:30", inserted?.timeLabel)
        assertTrue((inserted?.createdAtEpochMillis ?: 0L) > 0L)
    }

    @Test
    fun updateEntry_preservesIdentityAndAppliesTrimmedValues() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao())
        val existing =
            CalorieEntry(
                id = 7L,
                date = LocalDate.of(2026, 4, 10),
                calories = 200,
                foodName = "old",
                note = "old",
                timeLabel = "old",
                createdAt = LocalDateTime.of(2026, 4, 10, 8, 30),
            )

        repository.updateEntry(
            entry = existing,
            calories = 550,
            foodName = " 바나나 ",
            note = " 운동 후 ",
            timeLabel = " 오후 ",
        )

        val updated = calorieDao.lastUpdated
        assertNotNull(updated)
        assertEquals(7L, updated?.id)
        assertEquals(existing.date.toEpochDay(), updated?.dateEpochDay)
        assertEquals(550, updated?.calories)
        assertEquals("바나나", updated?.foodName)
        assertEquals("운동 후", updated?.note)
        assertEquals("오후", updated?.timeLabel)
        assertEquals(
            existing.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updated?.createdAtEpochMillis,
        )
    }

    @Test
    fun observePlan_mapsEntityToDomainModel() = runTest {
        val planDao = FakeMiniCutPlanDao()
        val repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), FakeDailyConditionCheckDao())

        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = LocalDate.of(2026, 4, 10).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = LocalDate.of(2026, 5, 7).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )

        val plan = repository.observePlan().first()
        assertNotNull(plan)
        assertEquals(LocalDate.of(2026, 4, 10), plan?.startDate)
        assertEquals(LocalDate.of(2026, 5, 7), plan?.endDate)
        assertEquals(1300, plan?.dailyTargetKcal)
    }

    @Test
    fun observeEntryAndSummaryFlows_mapDataForFeatureScreens() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao())
        val date = LocalDate.of(2026, 4, 10)

        calorieDao.seedEntries(
            date.toEpochDay(),
            listOf(
                CalorieEntryEntity(
                    id = 11L,
                    dateEpochDay = date.toEpochDay(),
                    calories = 500,
                    foodName = "닭가슴살",
                    note = "",
                    timeLabel = "점심",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 12, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )
        calorieDao.seedTotal(date.toEpochDay(), 500)
        calorieDao.seedDailySummaries(
            listOf(
                DailyCalorieSummaryRow(
                    dateEpochDay = date.toEpochDay(),
                    totalCalories = 500,
                    entryCount = 1,
                ),
                DailyCalorieSummaryRow(
                    dateEpochDay = LocalDate.of(2026, 4, 20).toEpochDay(),
                    totalCalories = 2000,
                    entryCount = 2,
                ),
            ),
        )

        val entries = repository.observeEntriesForDate(date).first()
        val total = repository.observeTodayTotal(date).first()
        val summaries = repository.observeDailySummaries(date.minusDays(1), date.plusDays(1)).first()

        assertEquals(1, entries.size)
        assertEquals("닭가슴살", entries.first().foodName)
        assertEquals(500, total)
        assertEquals(1, summaries.size)
        assertEquals(date, summaries.first().date)
        assertEquals(500, summaries.first().totalCalories)
    }

    @Test
    fun deleteEntry_forwardsIdToDao() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao())

        repository.deleteEntry(99L)

        assertEquals(99L, calorieDao.lastDeletedId)
    }

    @Test
    fun clearAllSavedData_clearsEntriesAndPlanTogether() = runTest {
        val planDao = FakeMiniCutPlanDao()
        val calorieDao = FakeCalorieEntryDao()
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val repository = MiniCutRepository(planDao, calorieDao, dailyConditionDao)

        repository.clearAllSavedData()

        assertEquals(1, calorieDao.deleteAllCalls)
        assertEquals(1, dailyConditionDao.deleteAllCalls)
        assertEquals(1, planDao.deletePlanCalls)
    }

    @Test
    fun observeRecentAndFavoritePresets_returnDeduplicatedRows() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao())
        val today = LocalDate.of(2026, 4, 10)

        calorieDao.seedEntries(
            today.toEpochDay(),
            listOf(
                CalorieEntryEntity(
                    id = 1L,
                    dateEpochDay = today.toEpochDay(),
                    calories = 430,
                    foodName = "닭가슴살",
                    note = "점심",
                    timeLabel = "12:30",
                    isFavorite = true,
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 12, 30)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                CalorieEntryEntity(
                    id = 2L,
                    dateEpochDay = today.toEpochDay(),
                    calories = 430,
                    foodName = "닭가슴살",
                    note = "점심",
                    timeLabel = "12:30",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                CalorieEntryEntity(
                    id = 3L,
                    dateEpochDay = today.toEpochDay(),
                    calories = 280,
                    foodName = "요거트",
                    note = "",
                    timeLabel = "아침",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 8, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        val recent = repository.observeRecentEntryPresets(limit = 4).first()
        val favorites = repository.observeFavoriteEntryPresets(limit = 4).first()

        assertEquals(2, recent.size)
        assertEquals("닭가슴살", recent.first().foodName)
        assertEquals("요거트", recent.last().foodName)
        assertEquals(1, favorites.size)
        assertEquals(true, favorites.first().isFavorite)
        assertEquals("닭가슴살", favorites.first().foodName)
    }

    @Test
    fun addEntryFromPresetAndUpdateFavorite_delegateToDao() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao())
        val today = LocalDate.of(2026, 4, 10)

        repository.addEntryFromPreset(
            date = today,
            preset =
                EntryQuickPreset(
                    foodName = "고구마",
                    calories = 300,
                    note = "운동 전",
                    timeLabel = "오전",
                    isFavorite = true,
                ),
        )
        repository.updateEntryFavorite(entryId = 5L, isFavorite = true)

        assertEquals("고구마", calorieDao.lastInserted?.foodName)
        assertEquals(300, calorieDao.lastInserted?.calories)
        assertEquals("운동 전", calorieDao.lastInserted?.note)
        assertEquals("오전", calorieDao.lastInserted?.timeLabel)
        assertEquals(5L to true, calorieDao.lastFavoriteUpdate)
    }

    @Test
    fun upsertDailyConditionCheck_andObserveInRange_workAsExpected() = runTest {
        val date = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), FakeCalorieEntryDao(), dailyConditionDao)

        repository.upsertDailyConditionCheck(
            date = date,
            bodyWeightKg = 78.4f,
            proteinGrams = 160,
            resistanceSets = 12,
            sleepHours = 7.0f,
            fatigueScore = 2,
            hungerScore = 3,
            moodScore = 4,
            workoutPerformanceScore = 4,
        )

        val oneDay = repository.observeDailyConditionCheck(date).first()
        val range = repository.observeDailyConditionChecks(date.minusDays(1), date.plusDays(1)).first()

        assertEquals(78.4f, oneDay?.bodyWeightKg)
        assertEquals(160, oneDay?.proteinGrams)
        assertEquals(12, oneDay?.resistanceSets)
        assertEquals(7.0f, oneDay?.sleepHours)
        assertEquals(2, oneDay?.fatigueScore)
        assertEquals(1, range.size)
        assertEquals(date, range.first().date)
    }

    @Test
    fun upsertDailyConditionCheck_ignoresNonPositiveOnlyInput() = runTest {
        val date = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val repository = MiniCutRepository(FakeMiniCutPlanDao(), FakeCalorieEntryDao(), dailyConditionDao)

        repository.upsertDailyConditionCheck(
            date = date,
            bodyWeightKg = 0f,
            proteinGrams = 0,
            resistanceSets = 0,
            sleepHours = 0f,
            fatigueScore = 0,
            hungerScore = 0,
            moodScore = 0,
            workoutPerformanceScore = 0,
        )

        val observed = repository.observeDailyConditionCheck(date).first()
        assertEquals(null, observed)
        assertEquals(null, dailyConditionDao.lastUpsert)
    }
}
