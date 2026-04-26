package com.minicut.timer.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minicut.timer.data.repository.MiniCutRepository
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.MiniCutPlan
import com.minicut.timer.ui.util.currentDateTickerFlow
import com.minicut.timer.ui.util.miniCutViewModelFactory
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val plan: MiniCutPlan? = null,
    val summaries: List<DailyCalorieSummary> = emptyList(),
    val monthLoggedDaysCount: Int = 0,
    val monthTotalCalories: Int = 0,
    val selectedDate: LocalDate? = null,
    val selectedEntries: List<CalorieEntry> = emptyList(),
    val selectedDayTotalCalories: Int = 0,
)

private data class CalendarMonthState(
    val currentDate: LocalDate,
    val month: YearMonth,
    val plan: MiniCutPlan?,
    val summaries: List<DailyCalorieSummary>,
)

private data class CalendarSelectionState(
    val selectedDate: LocalDate?,
    val selectedEntries: List<CalorieEntry>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: MiniCutRepository,
    dateTickerFlow: Flow<LocalDate> = currentDateTickerFlow(),
) : ViewModel() {
    private val currentDateFlow =
        dateTickerFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LocalDate.now(),
        )
    private val monthFlow = MutableStateFlow(YearMonth.from(currentDateFlow.value))
    private val selectedDateFlow = MutableStateFlow<LocalDate?>(null)
    private val monthlySummariesFlow =
        monthFlow.flatMapLatest { month ->
            repository.observeDailySummaries(
                startDate = month.atDay(1),
                endDate = month.atEndOfMonth(),
            )
        }
    private val selectedEntriesFlow =
        selectedDateFlow.flatMapLatest { date ->
            if (date == null) {
                flowOf(emptyList())
            } else {
                repository.observeEntriesForDate(date)
            }
        }

    init {
        viewModelScope.launch {
            var previousDate = currentDateFlow.value
            currentDateFlow.drop(1).collect { nextDate ->
                if (monthFlow.value == YearMonth.from(previousDate)) {
                    monthFlow.value = YearMonth.from(nextDate)
                }
                previousDate = nextDate
            }
        }
    }

    val uiState: StateFlow<CalendarUiState> =
        combine(
            combine(
                currentDateFlow,
                monthFlow,
                repository.observePlan(),
                monthlySummariesFlow,
            ) { currentDate, month, plan, summaries ->
                CalendarMonthState(
                    currentDate = currentDate,
                    month = month,
                    plan = plan,
                    summaries = summaries,
                )
            },
            combine(selectedDateFlow, selectedEntriesFlow) { selectedDate, selectedEntries ->
                CalendarSelectionState(
                    selectedDate = selectedDate,
                    selectedEntries = selectedEntries,
                )
            },
        ) { monthState, selectionState ->
            CalendarUiState(
                currentDate = monthState.currentDate,
                month = monthState.month,
                plan = monthState.plan,
                summaries = monthState.summaries,
                monthLoggedDaysCount = monthState.summaries.count { it.totalCalories > 0 },
                monthTotalCalories = monthState.summaries.sumOf { it.totalCalories },
                selectedDate = selectionState.selectedDate,
                selectedEntries = selectionState.selectedEntries,
                selectedDayTotalCalories = selectionState.selectedEntries.sumOf { it.calories },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(),
        )

    fun setMonth(month: YearMonth) {
        monthFlow.value = month
    }

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun clearSelection() {
        selectedDateFlow.value = null
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
        }
    }

    companion object {
        fun factory(repository: MiniCutRepository): ViewModelProvider.Factory =
            miniCutViewModelFactory { CalendarViewModel(repository) }
    }
}
