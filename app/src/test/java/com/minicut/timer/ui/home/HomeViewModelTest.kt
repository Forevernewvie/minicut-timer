package com.minicut.timer.ui.home

import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.DailyConditionCheckEntity
import com.minicut.timer.data.local.entity.MiniCutPlanEntity
import com.minicut.timer.data.repository.MiniCutRepository
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.CalorieAdjustmentDirection
import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.EntryQuickPreset
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.RecoveryRiskStatus
import com.minicut.timer.domain.model.StrengthTrendStatus
import com.minicut.timer.testing.FakeCalorieEntryDao
import com.minicut.timer.testing.FakeDailyConditionCheckDao
import com.minicut.timer.testing.FakeMiniCutPlanDao
import com.minicut.timer.testing.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_combinesPlanTotalsAndEntriesIntoDashboardValues() = runTest {
        val today = LocalDate.now()
        val planDao = FakeMiniCutPlanDao()
        val calorieDao = FakeCalorieEntryDao()
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, calorieDao, dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusDays(3).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = today.plusDays(24).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        calorieDao.seedTotal(today.toEpochDay(), 1450)
        calorieDao.seedEntries(
            today.toEpochDay(),
            listOf(
                CalorieEntryEntity(
                    id = 1L,
                    dateEpochDay = today.toEpochDay(),
                    calories = 900,
                    foodName = "아침",
                    note = "",
                    timeLabel = "08:00",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 8, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                CalorieEntryEntity(
                    id = 2L,
                    dateEpochDay = today.toEpochDay(),
                    calories = 550,
                    foodName = "점심",
                    note = "샐러드",
                    timeLabel = "12:30",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 12, 30)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(today, state.currentDate)
        assertEquals(1450, state.todayTotal)
        assertEquals(1300, state.todayTarget)
        assertEquals(0, state.remainingCalories)
        assertEquals(150, state.overCalories)
        assertEquals(CalorieRangeStatus.Above, state.todayRangeStatus)
        assertEquals(listOf("아침", "점심"), state.todayEntries.map { it.foodName })

        collectionJob.cancel()
    }

    @Test
    fun addUpdateAndDeleteEntry_forwardActionsThroughRepository() = runTest {
        val today = LocalDate.now()
        val calorieDao = FakeCalorieEntryDao()
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )

        viewModel.addEntry(
            calories = 420,
            foodName = " 닭가슴살 ",
            note = " 점심 ",
            timeLabel = " 12:00 ",
        )
        runCurrent()

        val inserted = calorieDao.lastInserted
        assertEquals(today.toEpochDay(), inserted?.dateEpochDay)
        assertEquals("닭가슴살", inserted?.foodName)
        assertEquals("점심", inserted?.note)
        assertEquals("12:00", inserted?.timeLabel)
        assertTrue((inserted?.createdAtEpochMillis ?: 0L) > 0L)

        viewModel.updateEntry(
            entry =
                CalorieEntry(
                    id = 5L,
                    date = today,
                    calories = 100,
                    foodName = "이전",
                    note = "이전",
                    timeLabel = "이전",
                    createdAt = LocalDateTime.of(2026, 4, 10, 9, 0),
                ),
            calories = 630,
            foodName = " 고구마 ",
            note = " 운동 전 ",
            timeLabel = " 오후 ",
        )
        runCurrent()

        val updated = calorieDao.lastUpdated
        assertEquals(5L, updated?.id)
        assertEquals(630, updated?.calories)
        assertEquals("고구마", updated?.foodName)
        assertEquals("운동 전", updated?.note)
        assertEquals("오후", updated?.timeLabel)

        viewModel.deleteEntry(5L)
        runCurrent()

        assertEquals(5L, calorieDao.lastDeletedId)
    }

    @Test
    fun uiState_exposesWeeklyReportAndQuickPresets() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val planDao = FakeMiniCutPlanDao()
        val calorieDao = FakeCalorieEntryDao()
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, calorieDao, dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusWeeks(4).toEpochDay(),
                durationWeeks = 2,
                endDateEpochDay = today.minusDays(1).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        calorieDao.seedEntries(
            today.toEpochDay(),
            listOf(
                CalorieEntryEntity(
                    id = 40L,
                    dateEpochDay = today.toEpochDay(),
                    calories = 340,
                    foodName = "고구마",
                    note = "운동 전",
                    timeLabel = "오전",
                    isFavorite = true,
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )
        calorieDao.seedDailySummaries(
            listOf(
                com.minicut.timer.data.local.query.DailyCalorieSummaryRow(today.minusDays(2).toEpochDay(), 1200, 2),
                com.minicut.timer.data.local.query.DailyCalorieSummaryRow(today.minusDays(1).toEpochDay(), 1500, 3),
                com.minicut.timer.data.local.query.DailyCalorieSummaryRow(today.toEpochDay(), 1000, 2),
            ),
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(MiniCutPhase.Completed, state.planPhase)
        assertEquals(1, state.favoritePresets.size)
        assertEquals("고구마", state.favoritePresets.first().foodName)
        assertEquals(1, state.recentPresets.size)
        assertEquals(3, state.weeklyReport.loggedDays)
        assertEquals(2, state.weeklyReport.adherentDays)

        collectionJob.cancel()
    }

    @Test
    fun addEntryFromPreset_andToggleFavorite_forwardToRepository() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val calorieDao = FakeCalorieEntryDao()
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )

        viewModel.addEntryFromPreset(
            EntryQuickPreset(
                foodName = "요거트",
                calories = 180,
                note = "",
                timeLabel = "아침",
                isFavorite = true,
            ),
        )
        viewModel.toggleFavorite(
            CalorieEntry(
                id = 88L,
                date = today,
                calories = 300,
                foodName = "샐러드",
                note = "",
                timeLabel = "점심",
                isFavorite = false,
                createdAt = LocalDateTime.of(2026, 4, 10, 12, 0),
            ),
        )
        runCurrent()

        assertEquals("요거트", calorieDao.lastInserted?.foodName)
        assertEquals(180, calorieDao.lastInserted?.calories)
        assertEquals(88L to true, calorieDao.lastFavoriteUpdate)
    }

    @Test
    fun saveDailyConditionCheck_updatesTrendAndProteinGuidance() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        dailyConditionDao.seedChecks(
            listOf(
                DailyConditionCheckEntity(
                    dateEpochDay = today.minusDays(7).toEpochDay(),
                    bodyWeightKg = 80f,
                    proteinGrams = 150,
                    resistanceSets = 8,
                    mainLiftKg = 100f,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 3, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )
        val planDao = FakeMiniCutPlanDao()
        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusDays(5).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = today.plusDays(20).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

        viewModel.saveDailyConditionCheck(
            bodyWeightKg = 79.2f,
            proteinGrams = 160,
            resistanceSets = 10,
            mainLiftKg = 105f,
            relapseTrigger = "야식",
            copingAction = "양치",
            sleepHours = 7.2f,
            fatigueScore = 2,
            hungerScore = 3,
            moodScore = 4,
            workoutPerformanceScore = 4,
        )
        runCurrent()

        assertEquals(today.toEpochDay(), dailyConditionDao.lastUpsert?.dateEpochDay)
        assertEquals(79.2f, dailyConditionDao.lastUpsert?.bodyWeightKg)
        assertEquals(160, dailyConditionDao.lastUpsert?.proteinGrams)
        assertEquals(10, dailyConditionDao.lastUpsert?.resistanceSets)
        assertEquals(105f, dailyConditionDao.lastUpsert?.mainLiftKg)
        assertEquals("야식", dailyConditionDao.lastUpsert?.relapseTrigger)
        assertEquals("양치", dailyConditionDao.lastUpsert?.copingAction)
        assertEquals(7.2f, dailyConditionDao.lastUpsert?.sleepHours)
        assertEquals(2, dailyConditionDao.lastUpsert?.fatigueScore)

        val state = viewModel.uiState.value
        assertEquals(158, state.recommendedProteinGrams)
        assertTrue((state.weeklyWeightTrend.ratePercentPerWeek ?: 0f) > 0f)
        assertEquals(CalorieAdjustmentDirection.Keep, state.calorieAdjustmentRecommendation.direction)
        assertEquals(1300, state.calorieAdjustmentRecommendation.suggestedTargetKcal)
        assertEquals(RecoveryRiskStatus.Stable, state.recoveryRiskAssessment.status)
        assertEquals(StrengthTrendStatus.Up, state.strengthTrend.status)
        assertEquals("야식", state.relapsePreventionInsight.recurringTrigger)
        collectionJob.cancel()
    }

    @Test
    fun saveDailyConditionCheck_withZeroOnlyValues_doesNotPersist() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val planDao = FakeMiniCutPlanDao()
        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusDays(5).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = today.plusDays(20).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )

        viewModel.saveDailyConditionCheck(
            bodyWeightKg = 0f,
            proteinGrams = 0,
            resistanceSets = 0,
            mainLiftKg = 0f,
            relapseTrigger = null,
            copingAction = null,
            sleepHours = 0f,
            fatigueScore = 0,
            hungerScore = 0,
            moodScore = 0,
            workoutPerformanceScore = 0,
        )
        runCurrent()

        assertEquals(null, dailyConditionDao.lastUpsert)
        assertEquals(null, viewModel.uiState.value.todayConditionCheck)
    }

    @Test
    fun uiState_suggestsLowerTargetWhenWeeklyWeightTrendIsTooSlow() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        dailyConditionDao.seedChecks(
            listOf(
                DailyConditionCheckEntity(
                    dateEpochDay = today.minusDays(7).toEpochDay(),
                    bodyWeightKg = 80f,
                    proteinGrams = 150,
                    resistanceSets = 8,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 3, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                DailyConditionCheckEntity(
                    dateEpochDay = today.toEpochDay(),
                    bodyWeightKg = 79.8f,
                    proteinGrams = 150,
                    resistanceSets = 8,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 10, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        val planDao = FakeMiniCutPlanDao()
        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusDays(5).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = today.plusDays(20).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
        runCurrent()

        val recommendation = viewModel.uiState.value.calorieAdjustmentRecommendation
        assertEquals(CalorieAdjustmentDirection.Decrease, recommendation.direction)
        assertEquals(1200, recommendation.suggestedTargetKcal)
        assertTrue(recommendation.actionable)
        collectionJob.cancel()
    }

    @Test
    fun uiState_suggestsHigherTargetWhenWeeklyWeightTrendIsTooFast() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        dailyConditionDao.seedChecks(
            listOf(
                DailyConditionCheckEntity(
                    dateEpochDay = today.minusDays(7).toEpochDay(),
                    bodyWeightKg = 80f,
                    proteinGrams = 160,
                    resistanceSets = 10,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 3, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                DailyConditionCheckEntity(
                    dateEpochDay = today.toEpochDay(),
                    bodyWeightKg = 78.8f,
                    proteinGrams = 170,
                    resistanceSets = 11,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 10, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        val planDao = FakeMiniCutPlanDao()
        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusDays(5).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = today.plusDays(20).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
        runCurrent()

        val recommendation = viewModel.uiState.value.calorieAdjustmentRecommendation
        assertEquals(CalorieAdjustmentDirection.Increase, recommendation.direction)
        assertEquals(1400, recommendation.suggestedTargetKcal)
        assertTrue(recommendation.actionable)
        collectionJob.cancel()
    }

    @Test
    fun uiState_prioritizesRecoveryRiskAdjustmentWhenRedFlagsAccumulate() = runTest {
        val today = LocalDate.of(2026, 4, 10)
        val dailyConditionDao = FakeDailyConditionCheckDao()
        dailyConditionDao.seedChecks(
            listOf(
                DailyConditionCheckEntity(
                    dateEpochDay = today.minusDays(2).toEpochDay(),
                    bodyWeightKg = 80f,
                    proteinGrams = 160,
                    resistanceSets = 10,
                    sleepHours = 5.5f,
                    fatigueScore = 4,
                    hungerScore = 4,
                    moodScore = 2,
                    workoutPerformanceScore = 2,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 8, 8, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                DailyConditionCheckEntity(
                    dateEpochDay = today.minusDays(1).toEpochDay(),
                    bodyWeightKg = 79.7f,
                    proteinGrams = 150,
                    resistanceSets = 8,
                    sleepHours = 5.0f,
                    fatigueScore = 5,
                    hungerScore = 4,
                    moodScore = 2,
                    workoutPerformanceScore = 2,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 9, 8, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                DailyConditionCheckEntity(
                    dateEpochDay = today.toEpochDay(),
                    bodyWeightKg = 79.5f,
                    proteinGrams = 150,
                    resistanceSets = 8,
                    sleepHours = 5.2f,
                    fatigueScore = 4,
                    hungerScore = 4,
                    moodScore = 2,
                    workoutPerformanceScore = 2,
                    updatedAtEpochMillis = LocalDateTime.of(2026, 4, 10, 8, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        val planDao = FakeMiniCutPlanDao()
        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = today.minusDays(5).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = today.plusDays(20).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        val viewModel =
            HomeViewModel(
                repository = MiniCutRepository(planDao, FakeCalorieEntryDao(), dailyConditionDao),
                dateTickerFlow = flowOf(today),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
        runCurrent()

        assertEquals(RecoveryRiskStatus.High, viewModel.uiState.value.recoveryRiskAssessment.status)
        val recommendation = viewModel.uiState.value.calorieAdjustmentRecommendation
        assertEquals(CalorieAdjustmentDirection.Increase, recommendation.direction)
        assertEquals(1400, recommendation.suggestedTargetKcal)
        assertTrue(recommendation.actionable)
        assertTrue(viewModel.uiState.value.dietBreakRecommendation.shouldSuggest)
        assertEquals(5, viewModel.uiState.value.dietBreakRecommendation.suggestedDays)
        collectionJob.cancel()
    }
}
