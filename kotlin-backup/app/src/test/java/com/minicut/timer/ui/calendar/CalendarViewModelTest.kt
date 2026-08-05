package com.minicut.timer.ui.calendar

import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.MiniCutPlanEntity
import com.minicut.timer.data.local.query.DailyCalorieSummaryRow
import com.minicut.timer.data.repository.MiniCutRepository
import com.minicut.timer.testing.FakeCalorieEntryDao
import com.minicut.timer.testing.FakeDailyConditionCheckDao
import com.minicut.timer.testing.FakeMiniCutPlanDao
import com.minicut.timer.testing.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_aggregatesMonthlySummariesAndSelectedDayEntries() = runTest {
        val month = YearMonth.of(2026, 4)
        val selectedDate = month.atDay(10)
        val planDao = FakeMiniCutPlanDao()
        val calorieDao = FakeCalorieEntryDao()
        val viewModel =
            CalendarViewModel(
                repository = MiniCutRepository(planDao, calorieDao, FakeDailyConditionCheckDao()),
                dateTickerFlow = flowOf(selectedDate),
            )
        val collectionJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

        planDao.planFlow.value =
            MiniCutPlanEntity(
                startDateEpochDay = month.atDay(1).toEpochDay(),
                durationWeeks = 4,
                endDateEpochDay = month.atDay(28).toEpochDay(),
                dailyTargetKcal = 1300,
                isActive = true,
            )
        calorieDao.seedDailySummaries(
            listOf(
                DailyCalorieSummaryRow(
                    dateEpochDay = selectedDate.toEpochDay(),
                    totalCalories = 1200,
                    entryCount = 2,
                ),
                DailyCalorieSummaryRow(
                    dateEpochDay = month.atDay(11).toEpochDay(),
                    totalCalories = 0,
                    entryCount = 0,
                ),
                DailyCalorieSummaryRow(
                    dateEpochDay = month.atDay(12).toEpochDay(),
                    totalCalories = 1500,
                    entryCount = 3,
                ),
                DailyCalorieSummaryRow(
                    dateEpochDay = month.plusMonths(1).atDay(1).toEpochDay(),
                    totalCalories = 800,
                    entryCount = 1,
                ),
            ),
        )
        calorieDao.seedEntries(
            selectedDate.toEpochDay(),
            listOf(
                CalorieEntryEntity(
                    id = 21L,
                    dateEpochDay = selectedDate.toEpochDay(),
                    calories = 600,
                    foodName = "샐러드",
                    note = "",
                    timeLabel = "점심",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 12, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
                CalorieEntryEntity(
                    id = 22L,
                    dateEpochDay = selectedDate.toEpochDay(),
                    calories = 650,
                    foodName = "고구마",
                    note = "간식",
                    timeLabel = "오후",
                    createdAtEpochMillis = LocalDateTime.of(2026, 4, 10, 15, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        viewModel.setMonth(month)
        viewModel.selectDate(selectedDate)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(month, state.month)
        assertEquals(2, state.monthLoggedDaysCount)
        assertEquals(2700, state.monthTotalCalories)
        assertEquals(selectedDate, state.selectedDate)
        assertEquals(listOf("샐러드", "고구마"), state.selectedEntries.map { it.foodName })
        assertEquals(1250, state.selectedDayTotalCalories)
        assertEquals(3, state.summaries.size)

        viewModel.clearSelection()
        runCurrent()

        assertNull(viewModel.uiState.value.selectedDate)
        assertEquals(emptyList<Any>(), viewModel.uiState.value.selectedEntries)
        assertEquals(0, viewModel.uiState.value.selectedDayTotalCalories)

        collectionJob.cancel()
    }

    @Test
    fun deleteEntry_forwardsIdToRepository() = runTest {
        val calorieDao = FakeCalorieEntryDao()
        val viewModel =
            CalendarViewModel(
                repository = MiniCutRepository(FakeMiniCutPlanDao(), calorieDao, FakeDailyConditionCheckDao()),
                dateTickerFlow = flowOf(LocalDate.of(2026, 4, 10)),
            )

        viewModel.deleteEntry(77L)
        runCurrent()

        assertEquals(77L, calorieDao.lastDeletedId)
    }
}
