package com.minicut.timer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minicut.timer.data.repository.MiniCutRepository
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.EntryQuickPreset
import com.minicut.timer.domain.model.MiniCutPlan
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.WeeklyAdherenceReport
import com.minicut.timer.domain.rules.MiniCutRules
import com.minicut.timer.ui.util.currentDateTickerFlow
import com.minicut.timer.ui.util.miniCutViewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val plan: MiniCutPlan? = null,
    val todayTotal: Int = 0,
    val todayTarget: Int = MiniCutRules.DEFAULT_TARGET_KCAL,
    val remainingCalories: Int = MiniCutRules.DEFAULT_TARGET_KCAL,
    val overCalories: Int = 0,
    val todayRangeStatus: CalorieRangeStatus = CalorieRangeStatus.NoData,
    val todayEntries: List<CalorieEntry> = emptyList(),
    val planPhase: MiniCutPhase? = null,
    val recentPresets: List<EntryQuickPreset> = emptyList(),
    val favoritePresets: List<EntryQuickPreset> = emptyList(),
    val weeklyReport: WeeklyAdherenceReport = WeeklyAdherenceReport(),
)

private data class HomePrimaryState(
    val plan: MiniCutPlan?,
    val currentDate: LocalDate,
    val total: Int,
    val entries: List<CalorieEntry>,
    val recentPresets: List<EntryQuickPreset>,
    val favoritePresets: List<EntryQuickPreset>,
)

private data class HomeCoreState(
    val plan: MiniCutPlan?,
    val currentDate: LocalDate,
    val total: Int,
    val entries: List<CalorieEntry>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: MiniCutRepository,
    dateTickerFlow: Flow<LocalDate> = currentDateTickerFlow(),
) : ViewModel() {
    private val currentDateFlow =
        dateTickerFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LocalDate.now(),
        )

    private val weeklySummariesFlow =
        currentDateFlow.flatMapLatest { date ->
            repository.observeDailySummaries(
                startDate = date.minusDays(6),
                endDate = date,
            )
        }

    private val coreStateFlow =
        combine(
            repository.observePlan(),
            currentDateFlow,
            currentDateFlow.flatMapLatest { date -> repository.observeTodayTotal(date) },
            currentDateFlow.flatMapLatest { date -> repository.observeEntriesForDate(date) },
        ) { plan, currentDate, total, entries ->
            HomeCoreState(
                plan = plan,
                currentDate = currentDate,
                total = total,
                entries = entries,
            )
        }

    private val presetsFlow =
        combine(
            repository.observeRecentEntryPresets(),
            repository.observeFavoriteEntryPresets(),
        ) { recentPresets, favoritePresets ->
            recentPresets to favoritePresets
        }

    private val primaryStateFlow =
        combine(coreStateFlow, presetsFlow) { core, presets ->
            HomePrimaryState(
                plan = core.plan,
                currentDate = core.currentDate,
                total = core.total,
                entries = core.entries,
                recentPresets = presets.first,
                favoritePresets = presets.second,
            )
        }

    val uiState: StateFlow<HomeUiState> =
        combine(primaryStateFlow, weeklySummariesFlow) { primaryState, weeklySummaries ->
            val target = primaryState.plan?.dailyTargetKcal ?: MiniCutRules.DEFAULT_TARGET_KCAL
            val planPhase = primaryState.plan?.let { MiniCutRules.phaseOf(it.startDate, it.endDate, primaryState.currentDate) }
            HomeUiState(
                currentDate = primaryState.currentDate,
                plan = primaryState.plan,
                todayTotal = primaryState.total,
                todayTarget = target,
                remainingCalories = MiniCutRules.remainingCalories(target, primaryState.total),
                overCalories = MiniCutRules.overCalories(target, primaryState.total),
                todayEntries = primaryState.entries,
                todayRangeStatus = MiniCutRules.targetStatus(primaryState.total, target),
                planPhase = planPhase,
                recentPresets = primaryState.recentPresets,
                favoritePresets = primaryState.favoritePresets,
                weeklyReport = MiniCutRules.weeklyAdherenceReport(weeklySummaries, target),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun addEntry(
        calories: Int,
        foodName: String,
        note: String,
        timeLabel: String,
    ) {
        viewModelScope.launch {
            repository.addEntry(
                date = currentDateFlow.value,
                calories = calories,
                foodName = foodName,
                note = note,
                timeLabel = timeLabel,
            )
        }
    }

    fun updateEntry(
        entry: CalorieEntry,
        calories: Int,
        foodName: String,
        note: String,
        timeLabel: String,
    ) {
        viewModelScope.launch {
            repository.updateEntry(
                entry = entry,
                calories = calories,
                foodName = foodName,
                note = note,
                timeLabel = timeLabel,
            )
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
        }
    }

    fun addEntryFromPreset(preset: EntryQuickPreset) {
        viewModelScope.launch {
            repository.addEntryFromPreset(
                date = currentDateFlow.value,
                preset = preset,
            )
        }
    }

    fun toggleFavorite(entry: CalorieEntry) {
        viewModelScope.launch {
            repository.updateEntryFavorite(
                entryId = entry.id,
                isFavorite = !entry.isFavorite,
            )
        }
    }

    companion object {
        fun factory(repository: MiniCutRepository): ViewModelProvider.Factory =
            miniCutViewModelFactory { HomeViewModel(repository) }
    }
}
