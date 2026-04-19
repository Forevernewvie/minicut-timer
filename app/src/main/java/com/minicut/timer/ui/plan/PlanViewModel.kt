package com.minicut.timer.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minicut.timer.data.repository.MiniCutRepository
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPlan
import com.minicut.timer.ui.util.miniCutViewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(
    private val repository: MiniCutRepository,
) : ViewModel() {
    val plan: StateFlow<MiniCutPlan?> =
        repository.observePlan().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun savePlan(
        startDate: LocalDate,
        durationWeeks: Int,
        dailyTargetKcal: Int,
        goalMode: MiniCutGoalMode,
    ) {
        viewModelScope.launch {
            repository.savePlan(startDate, durationWeeks, dailyTargetKcal, goalMode)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllSavedData()
        }
    }

    companion object {
        fun factory(repository: MiniCutRepository): ViewModelProvider.Factory =
            miniCutViewModelFactory { PlanViewModel(repository) }
    }
}
