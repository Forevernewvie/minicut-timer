package com.minicut.timer.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class MiniCutPlan(
    val startDate: LocalDate,
    val durationWeeks: Int,
    val endDate: LocalDate,
    val dailyTargetKcal: Int,
    val goalMode: MiniCutGoalMode = MiniCutGoalMode.MassReset,
    val activityLevel: ActivityLevel = ActivityLevel.Moderate,
    val estimatedMaintenanceKcal: Int = 0,
    val isActive: Boolean = true,
)

enum class ActivityLevel(
    val displayName: String,
    val kcalPerKgFactor: Float,
) {
    Low(
        displayName = "낮음(좌식 위주)",
        kcalPerKgFactor = 28f,
    ),
    Moderate(
        displayName = "보통(주 3~4회 활동)",
        kcalPerKgFactor = 31f,
    ),
    High(
        displayName = "높음(주 5회+ 훈련)",
        kcalPerKgFactor = 34f,
    ),
}

enum class MiniCutGoalMode(
    val displayName: String,
    val shortDescription: String,
) {
    MassReset(
        displayName = "매스업 리셋",
        shortDescription = "다음 벌크업 효율 회복",
    ),
    EventReady(
        displayName = "단기 외형 개선",
        shortDescription = "촬영·휴가 등 이벤트 대비",
    ),
}

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

data class ReverseDietStep(
    val weekLabel: String,
    val targetCalories: Int,
    val note: String,
)

data class ReverseDietPlan(
    val title: String,
    val summary: String,
    val caution: String,
    val steps: List<ReverseDietStep>,
)

data class DailyConditionCheck(
    val date: LocalDate,
    val bodyWeightKg: Float? = null,
    val proteinGrams: Int? = null,
    val resistanceSets: Int? = null,
    val sleepHours: Float? = null,
    val fatigueScore: Int? = null,
    val hungerScore: Int? = null,
    val moodScore: Int? = null,
    val workoutPerformanceScore: Int? = null,
    val updatedAt: LocalDateTime,
)

enum class WeeklyWeightTrendStatus {
    NoData,
    TooSlow,
    InRange,
    TooFast,
    GainOrStall,
}

data class WeeklyWeightTrend(
    val status: WeeklyWeightTrendStatus = WeeklyWeightTrendStatus.NoData,
    val ratePercentPerWeek: Float? = null,
    val message: String = "체중 기록이 2회 이상 쌓이면 주간 감량 속도를 계산할 수 있어요.",
)

enum class CalorieAdjustmentDirection {
    Keep,
    Increase,
    Decrease,
}

data class CalorieAdjustmentRecommendation(
    val currentTargetKcal: Int,
    val suggestedTargetKcal: Int,
    val direction: CalorieAdjustmentDirection,
    val deltaKcal: Int,
    val title: String,
    val message: String,
    val actionable: Boolean,
)

enum class DeficitRiskLevel {
    Unknown,
    Safe,
    Caution,
    High,
}

data class DeficitGuardrail(
    val maintenanceKcal: Int? = null,
    val deficitKcal: Int? = null,
    val deficitPercent: Float? = null,
    val level: DeficitRiskLevel = DeficitRiskLevel.Unknown,
    val title: String = "유지 칼로리 정보가 더 필요해요",
    val message: String = "체중과 활동 수준을 입력하면 결핍 강도를 안전 범위로 안내해드릴게요.",
    val canSave: Boolean = true,
)

enum class RecoveryRiskStatus {
    NoData,
    Stable,
    Watch,
    High,
}

data class RecoveryRiskAssessment(
    val status: RecoveryRiskStatus = RecoveryRiskStatus.NoData,
    val flaggedDays: Int = 0,
    val message: String = "수면·피로·허기 체크가 쌓이면 회복 리스크를 자동 분석해요.",
    val suggestDietBreak: Boolean = false,
)

enum class LeanMassProtectionGrade {
    NoData,
    Low,
    Moderate,
    Good,
    Excellent,
}

data class LeanMassProtectionScore(
    val score: Int = 0,
    val grade: LeanMassProtectionGrade = LeanMassProtectionGrade.NoData,
    val message: String = "단백질/훈련 체크가 쌓이면 근손실 방어 점수를 계산해요.",
    val proteinHitDays: Int = 0,
    val resistanceHitDays: Int = 0,
)

data class DietBreakRecommendation(
    val shouldSuggest: Boolean = false,
    val suggestedDays: Int = 0,
    val title: String = "다이어트 브레이크 불필요",
    val message: String = "현재는 계획한 감량 리듬을 유지해도 괜찮아요.",
)
