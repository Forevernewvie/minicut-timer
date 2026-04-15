package com.minicut.timer.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class MiniCutPlan(
    val startDate: LocalDate,
    val durationWeeks: Int,
    val endDate: LocalDate,
    val dailyTargetKcal: Int,
    val isActive: Boolean = true,
)

data class CalorieEntry(
    val id: Long = 0L,
    val date: LocalDate,
    val calories: Int,
    val foodName: String = "",
    val note: String = "",
    val timeLabel: String = "",
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime,
)

data class DailyCalorieSummary(
    val date: LocalDate,
    val totalCalories: Int,
    val entryCount: Int,
)

enum class CalorieRangeStatus {
    NoData,
    Below,
    InRange,
    Above,
}

enum class MiniCutPhase {
    Upcoming,
    Active,
    Completed,
}

data class EntryQuickPreset(
    val foodName: String,
    val calories: Int,
    val note: String = "",
    val timeLabel: String = "",
    val isFavorite: Boolean = false,
)

data class WeeklyAdherenceReport(
    val loggedDays: Int = 0,
    val adherentDays: Int = 0,
    val overTargetDays: Int = 0,
    val averageLoggedCalories: Int = 0,
    val focusMessage: String = "최근 7일 기록을 쌓으면 복기 카드가 채워집니다.",
)

enum class TargetGuidanceTone {
    Caution,
    Recommended,
    Flexible,
}

data class TargetGuidance(
    val title: String,
    val body: String,
    val footnote: String,
    val tone: TargetGuidanceTone,
)
