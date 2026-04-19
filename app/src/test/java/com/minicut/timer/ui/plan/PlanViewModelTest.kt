package com.minicut.timer.ui.plan

import com.minicut.timer.data.local.entity.MiniCutPlanEntity
import com.minicut.timer.data.repository.MiniCutRepository
import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.testing.FakeCalorieEntryDao
import com.minicut.timer.testing.FakeDailyConditionCheckDao
import com.minicut.timer.testing.FakeMiniCutPlanDao
import com.minicut.timer.testing.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun plan_stateReflectsRepositoryUpdates() = runTest {
        val planDao = FakeMiniCutPlanDao()
        val viewModel = PlanViewModel(MiniCutRepository(planDao, FakeCalorieEntryDao(), FakeDailyConditionCheckDao()))
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.plan.collect()
            }

        assertNull(viewModel.plan.value)

        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = LocalDate.of(2026, 4, 10).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = LocalDate.of(2026, 5, 7).toEpochDay(),
                dailyTargetKcal = 1400,
                isActive = true,
            )
        advanceUntilIdle()

        val state = viewModel.plan.value
        assertEquals(LocalDate.of(2026, 4, 10), state?.startDate)
        assertEquals(LocalDate.of(2026, 5, 7), state?.endDate)
        assertEquals(1400, state?.dailyTargetKcal)

        collectionJob.cancel()
    }

    @Test
    fun savePlan_andClearAllData_delegateToRepository() = runTest {
        val planDao = FakeMiniCutPlanDao()
        val calorieDao = FakeCalorieEntryDao()
        val dailyConditionDao = FakeDailyConditionCheckDao()
        val viewModel = PlanViewModel(MiniCutRepository(planDao, calorieDao, dailyConditionDao))

        viewModel.savePlan(
            startDate = LocalDate.of(2026, 4, 10),
            durationWeeks = 4,
            dailyTargetKcal = 1500,
            goalMode = MiniCutGoalMode.EventReady,
            activityLevel = ActivityLevel.High,
            estimatedMaintenanceKcal = 2500,
        )
        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 4, 10).toEpochDay(), planDao.lastUpsert?.startDateEpochDay)
        assertEquals(LocalDate.of(2026, 5, 7).toEpochDay(), planDao.lastUpsert?.endDateEpochDay)
        assertEquals(1500, planDao.lastUpsert?.dailyTargetKcal)
        assertEquals(MiniCutGoalMode.EventReady.name, planDao.lastUpsert?.goalMode)
        assertEquals(ActivityLevel.High.name, planDao.lastUpsert?.activityLevel)
        assertEquals(2500, planDao.lastUpsert?.estimatedMaintenanceKcal)

        viewModel.clearAllData()
        advanceUntilIdle()

        assertEquals(1, calorieDao.deleteAllCalls)
        assertEquals(1, dailyConditionDao.deleteAllCalls)
        assertEquals(1, planDao.deletePlanCalls)
        assertNull(planDao.planFlow.value)
    }
}
