package com.minicut.timer.domain.rules

import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.CalorieAdjustmentDirection
import com.minicut.timer.domain.model.CalorieAdjustmentRecommendation
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.ReverseDietPlan
import com.minicut.timer.domain.model.ReverseDietStep
import com.minicut.timer.domain.model.TargetGuidance
import com.minicut.timer.domain.model.TargetGuidanceTone
import com.minicut.timer.domain.model.WeeklyWeightTrend
import com.minicut.timer.domain.model.WeeklyWeightTrendStatus
import com.minicut.timer.domain.model.WeeklyAdherenceReport
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object MiniCutRules {
    const val MIN_WEEKS = 2
    const val MAX_WEEKS = 6
    const val RECOMMENDED_MIN_KCAL = 1000
    const val RECOMMENDED_MAX_KCAL = 1500
    val TARGET_OPTIONS_KCAL = listOf(1000, 1200, 1300, 1400, 1500)
    const val DEFAULT_TARGET_KCAL = 1300

    fun isValidDuration(durationWeeks: Int): Boolean = durationWeeks in MIN_WEEKS..MAX_WEEKS

    fun isValidTarget(targetKcal: Int): Boolean = targetKcal in RECOMMENDED_MIN_KCAL..RECOMMENDED_MAX_KCAL

    fun calculateEndDate(startDate: LocalDate, durationWeeks: Int): LocalDate {
        require(isValidDuration(durationWeeks)) { "미니컷 기간은 2~6주만 허용됩니다." }
        return startDate.plusWeeks(durationWeeks.toLong()).minusDays(1)
    }

    fun calculateProgress(
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Float {
        if (today.isBefore(startDate)) return 0f
        if (today.isAfter(endDate)) return 1f
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toFloat() + 1f
        val elapsedDays = ChronoUnit.DAYS.between(startDate, today).toFloat() + 1f
        return (elapsedDays / totalDays).coerceIn(0f, 1f)
    }

    fun isDateInsidePlan(
        date: LocalDate,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    fun remainingDays(
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Int =
        when {
            today.isBefore(startDate) -> ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            today.isAfter(endDate) -> 0
            else -> ChronoUnit.DAYS.between(today, endDate).toInt() + 1
        }

    fun phaseOf(
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): MiniCutPhase =
        when {
            today.isBefore(startDate) -> MiniCutPhase.Upcoming
            today.isAfter(endDate) -> MiniCutPhase.Completed
            else -> MiniCutPhase.Active
        }

    fun remainingCalories(
        targetCalories: Int,
        consumedCalories: Int,
    ): Int = (targetCalories - consumedCalories).coerceAtLeast(0)

    fun overCalories(
        targetCalories: Int,
        consumedCalories: Int,
    ): Int = (consumedCalories - targetCalories).coerceAtLeast(0)

    fun isOverTarget(
        targetCalories: Int,
        consumedCalories: Int,
    ): Boolean = consumedCalories > targetCalories

    fun targetStatus(
        totalCalories: Int?,
        targetCalories: Int,
    ): CalorieRangeStatus =
        when {
            totalCalories == null || totalCalories <= 0 -> CalorieRangeStatus.NoData
            totalCalories < targetCalories -> CalorieRangeStatus.Below
            totalCalories == targetCalories -> CalorieRangeStatus.InRange
            else -> CalorieRangeStatus.Above
        }

    fun rangeStatus(totalCalories: Int?): CalorieRangeStatus =
        when {
            totalCalories == null || totalCalories <= 0 -> CalorieRangeStatus.NoData
            totalCalories < RECOMMENDED_MIN_KCAL -> CalorieRangeStatus.Below
            totalCalories <= RECOMMENDED_MAX_KCAL -> CalorieRangeStatus.InRange
            else -> CalorieRangeStatus.Above
        }

    fun weeklyAdherenceReport(
        summaries: List<DailyCalorieSummary>,
        targetCalories: Int,
    ): WeeklyAdherenceReport {
        val loggedSummaries = summaries.filter { it.totalCalories > 0 }
        val loggedDays = loggedSummaries.size
        val adherentDays = loggedSummaries.count { it.totalCalories <= targetCalories }
        val overTargetDays = loggedSummaries.count { it.totalCalories > targetCalories }
        val averageLoggedCalories = if (loggedDays == 0) 0 else loggedSummaries.sumOf { it.totalCalories } / loggedDays
        val focusMessage =
            when {
                loggedDays == 0 -> "최근 7일 기록이 없어요. 오늘 한 끼만 적어도 유지/복기 흐름이 다시 살아납니다."
                adherentDays >= 5 && overTargetDays == 0 -> "이번 주는 흐름이 안정적이에요. 종료 후에는 유지 칼로리로 천천히 올릴 준비를 해보세요."
                overTargetDays >= 3 -> "초과한 날이 많았어요. 주말·야식처럼 반복되는 한 패턴만 줄여도 다음 주가 편해집니다."
                else -> "기록은 잘 이어지고 있어요. 초과한 날 다음 끼니를 가볍게 맞추는 식으로 리듬을 정리해보세요."
            }

        return WeeklyAdherenceReport(
            loggedDays = loggedDays,
            adherentDays = adherentDays,
            overTargetDays = overTargetDays,
            averageLoggedCalories = averageLoggedCalories,
            focusMessage = focusMessage,
        )
    }

    fun targetGuidance(
        targetCalories: Int,
        durationWeeks: Int,
    ): TargetGuidance =
        when {
            targetCalories <= 1100 || (durationWeeks >= 5 && targetCalories <= 1200) ->
                TargetGuidance(
                    title = "낮은 목표예요",
                    body = "긴 플랜에서 너무 낮은 목표는 피로감과 폭식을 부르기 쉬워요. 5~6주라면 한 단계 높게 잡는 편이 안정적입니다.",
                    footnote = "컨디션 저하, 수면 흔들림, 운동 회복 저하가 느껴지면 바로 상향하세요.",
                    tone = TargetGuidanceTone.Caution,
                )

            targetCalories >= 1500 ->
                TargetGuidance(
                    title = "상단 범위 목표예요",
                    body = "활동량이 높거나 종료 직후 유지 모드로 넘어갈 때 무리 없는 선택이 될 수 있어요.",
                    footnote = "감량 체감이 약하면 기록 정확도와 간식 빈도를 먼저 점검하세요.",
                    tone = TargetGuidanceTone.Flexible,
                )

            else ->
                TargetGuidance(
                    title = "권장 범위에 들어왔어요",
                    body = "2~6주 미니컷에 가장 무난한 구간입니다. 기록과 복기가 이어지기 쉬운 목표예요.",
                    footnote = "배고픔이 심하지 않고 기록을 꾸준히 남길 수 있는 수준이면 가장 좋습니다.",
                    tone = TargetGuidanceTone.Recommended,
                )
        }

    fun reverseDietPlan(
        dailyTargetKcal: Int,
        goalMode: MiniCutGoalMode,
    ): ReverseDietPlan {
        val weeklyStep = when (goalMode) {
            MiniCutGoalMode.MassReset -> 120
            MiniCutGoalMode.EventReady -> 150
        }
        val summary = when (goalMode) {
            MiniCutGoalMode.MassReset -> "리셋 목적이라면 2~3주에 걸쳐 섭취량을 천천히 올리며 다음 벌크업 준비를 시작하세요."
            MiniCutGoalMode.EventReady -> "단기 외형 목적이었더라도 종료 직후 급반등을 막으려면 단계적으로 유지 칼로리로 복귀해야 합니다."
        }
        val caution = "체중이 3~4일 연속 급상승하거나 폭식 충동이 커지면 증가 폭을 잠시 줄이고 수면/스트레스부터 안정화하세요."
        val steps =
            (1..3).map { week ->
                val target = dailyTargetKcal + weeklyStep * week
                ReverseDietStep(
                    weekLabel = "${week}주차",
                    targetCalories = target,
                    note =
                        if (week == 1) {
                            "식사 리듬을 먼저 안정화하고 저항운동 빈도를 유지하세요."
                        } else {
                            "체중·컨디션 추세를 보며 유지 칼로리에 가깝게 올리세요."
                        },
                )
            }
        return ReverseDietPlan(
            title = "종료 후 리버스 다이어트",
            summary = summary,
            caution = caution,
            steps = steps,
        )
    }

    fun recommendedProteinGrams(weightKg: Float?): Int? =
        weightKg?.takeIf { it > 0f }?.let { (it * 2f).roundToInt() }

    fun weeklyWeightTrend(
        checks: List<DailyConditionCheck>,
    ): WeeklyWeightTrend {
        val weighted = checks.filter { (it.bodyWeightKg ?: 0f) > 0f }.sortedBy { it.date }
        if (weighted.size < 2) {
            return WeeklyWeightTrend()
        }

        val first = weighted.first()
        val last = weighted.last()
        val startWeight = first.bodyWeightKg ?: return WeeklyWeightTrend()
        val endWeight = last.bodyWeightKg ?: return WeeklyWeightTrend()
        if (startWeight <= 0f) return WeeklyWeightTrend()
        val daySpan = ChronoUnit.DAYS.between(first.date, last.date).toInt()
        if (daySpan < 3) {
            return WeeklyWeightTrend(
                message = "최소 3일 이상 간격의 체중 2회 기록이 있어야 속도 판단이 정확해져요.",
            )
        }

        val lossPercentOverPeriod = ((startWeight - endWeight) / startWeight) * 100f
        val normalizedWeeklyLoss = lossPercentOverPeriod * (7f / daySpan.toFloat())
        val rounded = (normalizedWeeklyLoss * 100f).roundToInt() / 100f

        return when {
            normalizedWeeklyLoss <= 0f ->
                WeeklyWeightTrend(
                    status = WeeklyWeightTrendStatus.GainOrStall,
                    ratePercentPerWeek = rounded,
                    message = "최근 체중이 정체/증가 추세예요. 간식·야식 패턴과 기록 정확도를 먼저 점검하세요.",
                )

            normalizedWeeklyLoss < 0.75f ->
                WeeklyWeightTrend(
                    status = WeeklyWeightTrendStatus.TooSlow,
                    ratePercentPerWeek = rounded,
                    message = "주간 감량 속도가 느린 편이에요. 활동량·기록 누락을 점검하고 다음 주에 미세 조정해보세요.",
                )

            normalizedWeeklyLoss <= 1.25f ->
                WeeklyWeightTrend(
                    status = WeeklyWeightTrendStatus.InRange,
                    ratePercentPerWeek = rounded,
                    message = "권장 감량 속도(주당 0.75~1.25%) 범위에 있어요. 현재 리듬을 유지하세요.",
                )

            else ->
                WeeklyWeightTrend(
                    status = WeeklyWeightTrendStatus.TooFast,
                    ratePercentPerWeek = rounded,
                    message = "감량 속도가 너무 빨라요. 근손실/반동 위험을 줄이기 위해 섭취량을 소폭 올리는 것을 권장합니다.",
                )
        }
    }

    fun calorieAdjustmentRecommendation(
        currentTargetKcal: Int,
        weeklyWeightTrend: WeeklyWeightTrend,
    ): CalorieAdjustmentRecommendation {
        val clampedCurrent = currentTargetKcal.coerceIn(RECOMMENDED_MIN_KCAL, RECOMMENDED_MAX_KCAL)
        val targetOptions = TARGET_OPTIONS_KCAL.sorted()
        val nextLowerOption = targetOptions.lastOrNull { it < clampedCurrent }
        val nextHigherOption = targetOptions.firstOrNull { it > clampedCurrent }
        return when (weeklyWeightTrend.status) {
            WeeklyWeightTrendStatus.NoData ->
                CalorieAdjustmentRecommendation(
                    currentTargetKcal = clampedCurrent,
                    suggestedTargetKcal = clampedCurrent,
                    direction = CalorieAdjustmentDirection.Keep,
                    deltaKcal = 0,
                    title = "데이터 수집 중",
                    message = "체중 체크인이 더 쌓이면 목표 칼로리 미세 조정 제안을 자동으로 제공합니다.",
                    actionable = false,
                )

            WeeklyWeightTrendStatus.InRange ->
                CalorieAdjustmentRecommendation(
                    currentTargetKcal = clampedCurrent,
                    suggestedTargetKcal = clampedCurrent,
                    direction = CalorieAdjustmentDirection.Keep,
                    deltaKcal = 0,
                    title = "현재 목표 유지",
                    message = "감량 속도가 권장 범위입니다. 이번 주는 목표 칼로리를 유지하세요.",
                    actionable = false,
                )

            WeeklyWeightTrendStatus.TooSlow,
            WeeklyWeightTrendStatus.GainOrStall,
            -> {
                val suggested = nextLowerOption ?: clampedCurrent
                val delta = clampedCurrent - suggested
                CalorieAdjustmentRecommendation(
                    currentTargetKcal = clampedCurrent,
                    suggestedTargetKcal = suggested,
                    direction = if (delta > 0) CalorieAdjustmentDirection.Decrease else CalorieAdjustmentDirection.Keep,
                    deltaKcal = delta,
                    title = if (delta > 0) "목표 칼로리 소폭 하향 제안" else "최저 구간 도달",
                    message =
                        if (delta > 0) {
                            "다음 7일은 하루 목표를 ${delta}kcal 낮춰 반응을 확인해보세요."
                        } else {
                            "이미 권장 하한(1000kcal)에 있어요. 칼로리 추가 하향보다 기록 정확도·활동량을 먼저 점검하세요."
                        },
                    actionable = delta > 0,
                )
            }

            WeeklyWeightTrendStatus.TooFast -> {
                val suggested = nextHigherOption ?: clampedCurrent
                val delta = suggested - clampedCurrent
                CalorieAdjustmentRecommendation(
                    currentTargetKcal = clampedCurrent,
                    suggestedTargetKcal = suggested,
                    direction = if (delta > 0) CalorieAdjustmentDirection.Increase else CalorieAdjustmentDirection.Keep,
                    deltaKcal = delta,
                    title = if (delta > 0) "목표 칼로리 소폭 상향 제안" else "상한 구간 도달",
                    message =
                        if (delta > 0) {
                            "근손실/피로 위험을 줄이기 위해 다음 7일은 하루 목표를 ${delta}kcal 올려보세요."
                        } else {
                            "이미 상단 목표(1500kcal)입니다. 피로가 크면 휴식·수면·훈련 볼륨부터 조정하세요."
                        },
                    actionable = delta > 0,
                )
            }
        }
    }
}
