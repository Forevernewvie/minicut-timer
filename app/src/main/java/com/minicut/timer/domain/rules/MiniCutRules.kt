package com.minicut.timer.domain.rules

import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.CalendarRhythmStatus
import com.minicut.timer.domain.model.CalendarRhythmSummary
import com.minicut.timer.domain.model.CalorieAdjustmentDirection
import com.minicut.timer.domain.model.CalorieAdjustmentRecommendation
import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.DeficitGuardrail
import com.minicut.timer.domain.model.DeficitRiskLevel
import com.minicut.timer.domain.model.DietBreakRecommendation
import com.minicut.timer.domain.model.LeanMassProtectionGrade
import com.minicut.timer.domain.model.LeanMassProtectionScore
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.MiniCutPlan
import com.minicut.timer.domain.model.MissionType
import com.minicut.timer.domain.model.PlanProgressSnapshot
import com.minicut.timer.domain.model.RecoveryRiskAssessment
import com.minicut.timer.domain.model.RecoveryRiskStatus
import com.minicut.timer.domain.model.RelapsePreventionInsight
import com.minicut.timer.domain.model.ReverseDietPlan
import com.minicut.timer.domain.model.ReverseDietStep
import com.minicut.timer.domain.model.StrengthTrend
import com.minicut.timer.domain.model.StrengthTrendStatus
import com.minicut.timer.domain.model.TargetGuidance
import com.minicut.timer.domain.model.TargetGuidanceTone
import com.minicut.timer.domain.model.TodayMission
import com.minicut.timer.domain.model.WeeklyAdherenceReport
import com.minicut.timer.domain.model.WeeklyCoachingSnapshot
import com.minicut.timer.domain.model.WeeklyWeightTrend
import com.minicut.timer.domain.model.WeeklyWeightTrendStatus
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
    private const val HIGH_DEFICIT_PERCENT = 35f
    private const val CAUTION_DEFICIT_PERCENT = 25f
    private const val HIGH_DEFICIT_KCAL = 900
    private const val CAUTION_DEFICIT_KCAL = 700

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

    fun planProgressSnapshot(
        plan: MiniCutPlan,
        currentDate: LocalDate,
    ): PlanProgressSnapshot {
        val phase = phaseOf(plan.startDate, plan.endDate, currentDate)
        val progress = calculateProgress(plan.startDate, plan.endDate, currentDate)
        val totalDays = ChronoUnit.DAYS.between(plan.startDate, plan.endDate).toInt() + 1
        val elapsedDays =
            ChronoUnit.DAYS.between(plan.startDate, currentDate)
                .toInt()
                .plus(1)
                .coerceIn(0, totalDays)
        val remainingDays = remainingDays(plan.startDate, plan.endDate, currentDate)
        val daysUntilStart = ChronoUnit.DAYS.between(currentDate, plan.startDate).toInt().coerceAtLeast(0)
        val dDayLabel =
            when (phase) {
                MiniCutPhase.Upcoming -> "D-${daysUntilStart}"
                MiniCutPhase.Active -> if (currentDate == plan.endDate) "D-day" else "D-${remainingDays}"
                MiniCutPhase.Completed -> "완료"
            }
        val headline =
            when (phase) {
                MiniCutPhase.Upcoming -> "${daysUntilStart}일 뒤 미니컷 시작"
                MiniCutPhase.Active -> "${plan.durationWeeks}주 플랜 진행 중"
                MiniCutPhase.Completed -> "미니컷 기간 완료"
            }
        val supportingText =
            when (phase) {
                MiniCutPhase.Upcoming -> "시작 전에는 기록 루틴과 하루 기준만 준비하면 충분해요."
                MiniCutPhase.Active -> "오늘까지 ${elapsedDays}일째예요. 끝나는 날짜가 정해져 있으니 오늘 행동만 마무리하세요."
                MiniCutPhase.Completed -> "이제 급하게 더 줄이기보다 유지·리버스 전환을 점검할 시점입니다."
            }

        return PlanProgressSnapshot(
            phase = phase,
            progress = progress,
            elapsedDays = elapsedDays,
            remainingDays = remainingDays,
            dDayLabel = dDayLabel,
            headline = headline,
            supportingText = supportingText,
        )
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

    fun todayMissions(
        hasFoodLog: Boolean,
        hasCoachCheckIn: Boolean,
        weeklyReport: WeeklyAdherenceReport,
    ): List<TodayMission> =
        listOf(
            TodayMission(
                type = MissionType.FoodLog,
                title = "오늘 음식 1개 기록",
                description = if (hasFoodLog) "오늘 섭취 흐름이 시작됐어요." else "첫 식사만 적어도 남은 칼로리가 바로 보입니다.",
                actionLabel = if (hasFoodLog) "기록 완료" else "음식 기록하기",
                isComplete = hasFoodLog,
            ),
            TodayMission(
                type = MissionType.CoachCheckIn,
                title = "3분 코칭 체크인",
                description = if (hasCoachCheckIn) "오늘 회복·근력 신호가 반영됐어요." else "체중·단백질·저항운동 중 아는 것만 입력하세요.",
                actionLabel = if (hasCoachCheckIn) "체크인 완료" else "체크인 열기",
                isComplete = hasCoachCheckIn,
            ),
            TodayMission(
                type = MissionType.WeeklyReview,
                title = "이번 주 리듬 확인",
                description =
                    if (weeklyReport.loggedDays >= 3) {
                        "최근 7일 기록 흐름을 읽을 만큼 데이터가 모였어요."
                    } else {
                        "주 3회 이상 기록하면 복기 품질이 확 올라갑니다."
                    },
                actionLabel = "주간 복기 보기",
                isComplete = weeklyReport.loggedDays >= 3,
            ),
        )

    fun weeklyCoachingSnapshot(
        weeklyReport: WeeklyAdherenceReport,
        recoveryRisk: RecoveryRiskAssessment,
        strengthTrend: StrengthTrend,
        dietBreakRecommendation: DietBreakRecommendation,
    ): WeeklyCoachingSnapshot {
        val nextAction =
            when {
                dietBreakRecommendation.shouldSuggest -> "이번 주는 감량 강도보다 ${dietBreakRecommendation.suggestedDays}일 유지 전환을 먼저 검토하세요."
                recoveryRisk.status == RecoveryRiskStatus.High -> "수면·피로·허기 신호가 높습니다. 오늘 체크인 후 목표 완화를 검토하세요."
                weeklyReport.loggedDays < 3 -> "다음 목표는 완벽한 식단이 아니라 주 3회 기록 리듬 만들기입니다."
                weeklyReport.overTargetDays >= 3 -> "초과가 반복됐습니다. 가장 자주 초과한 시간대나 상황을 한 가지 줄여보세요."
                weeklyReport.adherentDays >= 5 -> "목표 이내 흐름이 좋습니다. 지금은 크게 바꾸지 말고 루틴을 유지하세요."
                strengthTrend.status == StrengthTrendStatus.Down -> "근력 하락 신호가 있습니다. 훈련 볼륨과 회복을 먼저 점검하세요."
                else -> "오늘은 음식 1개 기록과 3분 체크인만 마무리해도 충분합니다."
            }
        val momentumLabel =
            when {
                weeklyReport.loggedDays >= 6 -> "리듬 강함"
                weeklyReport.loggedDays >= 3 -> "리듬 형성 중"
                weeklyReport.loggedDays > 0 -> "시작됨"
                else -> "대기"
            }
        val momentumMessage =
            when {
                weeklyReport.loggedDays >= 6 -> "거의 매일 앱을 활용하고 있어요. 복기 품질이 가장 좋은 구간입니다."
                weeklyReport.loggedDays >= 3 -> "이번 주 기록 리듬이 생겼어요. 체크인까지 더하면 코칭 정확도가 올라갑니다."
                weeklyReport.loggedDays > 0 -> "첫 기록이 들어왔어요. 이번 주 3회 기록을 목표로 잡아보세요."
                else -> "아직 이번 주 기록이 없어요. 한 끼만 적어도 리포트가 살아납니다."
            }

        return WeeklyCoachingSnapshot(
            title = "이번 주 코칭 스냅샷",
            summary = "기록 ${weeklyReport.loggedDays}/7일 · 목표 이내 ${weeklyReport.adherentDays}일 · 초과 ${weeklyReport.overTargetDays}일",
            nextAction = nextAction,
            momentumLabel = momentumLabel,
            momentumMessage = momentumMessage,
        )
    }

    fun calendarRhythmStatus(
        totalCalories: Int,
        targetCalories: Int,
    ): CalendarRhythmStatus =
        when {
            totalCalories <= 0 -> CalendarRhythmStatus.Empty
            totalCalories > targetCalories -> CalendarRhythmStatus.OverTarget
            else -> CalendarRhythmStatus.WithinTarget
        }

    fun calendarRhythmSummary(
        summaries: List<DailyCalorieSummary>,
        checks: List<DailyConditionCheck>,
        targetCalories: Int,
    ): CalendarRhythmSummary {
        val loggedDays = summaries.count { it.totalCalories > 0 }
        val withinTargetDays = summaries.count { it.totalCalories > 0 && it.totalCalories <= targetCalories }
        val overTargetDays = summaries.count { it.totalCalories > targetCalories }
        val checkInDays = checks.count()
        val message =
            when {
                loggedDays == 0 && checkInDays == 0 -> "아직 월간 리듬이 비어 있어요. 오늘 한 끼와 체크인 하나만 남겨보세요."
                loggedDays >= 20 -> "이번 달 기록 리듬이 매우 안정적이에요. 복기에서 패턴을 읽기 좋습니다."
                loggedDays >= 10 -> "기록한 날이 충분히 쌓이고 있어요. 초과일과 체크인일을 함께 비교해보세요."
                loggedDays > 0 -> "기록이 시작됐어요. 빈 날도 실패가 아니라 리듬을 조정할 단서입니다."
                else -> "체크인은 시작됐어요. 음식 기록까지 더하면 칼로리 리듬이 보입니다."
            }
        return CalendarRhythmSummary(
            loggedDays = loggedDays,
            withinTargetDays = withinTargetDays,
            overTargetDays = overTargetDays,
            checkInDays = checkInDays,
            message = message,
        )
    }

    fun stateAwareReminderMessage(
        isEvening: Boolean,
        currentDate: LocalDate,
        recentSummaries: List<DailyCalorieSummary>,
        recentChecks: List<DailyConditionCheck>,
    ): String {
        val todaySummary = recentSummaries.firstOrNull { it.date == currentDate }
        val yesterdaySummary = recentSummaries.firstOrNull { it.date == currentDate.minusDays(1) }
        val recoveryRisk = recoveryRiskAssessment(recentChecks)
        return when {
            recoveryRisk.status == RecoveryRiskStatus.High ->
                "최근 회복 신호가 높아요. 오늘은 더 줄이기보다 3분 체크인으로 수면·피로를 먼저 확인하세요."
            isEvening && (todaySummary?.totalCalories ?: 0) == 0 ->
                "오늘 기록이 아직 없어요. 한 끼만 적어도 내일 복기 흐름이 살아납니다."
            !isEvening && (yesterdaySummary?.totalCalories ?: 0) == 0 ->
                "어제 기록이 비었어요. 오늘은 첫 식사 하나만 가볍게 남기며 리듬을 다시 잡아보세요."
            recentSummaries.count { it.totalCalories > 0 } >= 5 ->
                "이번 주 기록 리듬이 좋아요. 오늘도 짧게 확인하고 플랜 완주에 한 걸음 더 가까워지세요."
            isEvening ->
                "짧은 미니컷일수록 마무리가 중요해요. 오늘 섭취와 체크인을 정리해보세요."
            else ->
                "오늘도 짧고 선명하게. 첫 식사부터 가볍게 기록해보세요."
        }
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

    fun estimateMaintenanceCalories(
        bodyWeightKg: Float?,
        activityLevel: ActivityLevel,
    ): Int? =
        bodyWeightKg
            ?.takeIf { it > 0f }
            ?.let { (it * activityLevel.kcalPerKgFactor).roundToInt().coerceIn(1400, 4200) }

    fun deficitGuardrail(
        targetKcal: Int,
        maintenanceKcal: Int?,
    ): DeficitGuardrail {
        val normalizedMaintenance = maintenanceKcal?.takeIf { it > 0 } ?: return DeficitGuardrail()
        val deficit = (normalizedMaintenance - targetKcal).coerceAtLeast(0)
        val deficitPercent = if (normalizedMaintenance == 0) 0f else (deficit * 100f / normalizedMaintenance.toFloat())
        val roundedPercent = (deficitPercent * 10f).roundToInt() / 10f

        return when {
            deficitPercent >= HIGH_DEFICIT_PERCENT || deficit >= HIGH_DEFICIT_KCAL ->
                DeficitGuardrail(
                    maintenanceKcal = normalizedMaintenance,
                    deficitKcal = deficit,
                    deficitPercent = roundedPercent,
                    level = DeficitRiskLevel.High,
                    title = "결핍 강도가 과도해요",
                    message = "현재 설정은 유지 대비 ${deficit}kcal(${roundedPercent}%) 결핍으로 추정됩니다. 근손실·피로 리스크를 줄이려면 목표를 높여주세요.",
                    canSave = false,
                )

            deficitPercent >= CAUTION_DEFICIT_PERCENT || deficit >= CAUTION_DEFICIT_KCAL ->
                DeficitGuardrail(
                    maintenanceKcal = normalizedMaintenance,
                    deficitKcal = deficit,
                    deficitPercent = roundedPercent,
                    level = DeficitRiskLevel.Caution,
                    title = "결핍 강도 주의 구간",
                    message = "유지 대비 ${deficit}kcal(${roundedPercent}%) 결핍입니다. 수면·훈련 회복 신호를 더 자주 확인하세요.",
                    canSave = true,
                )

            else ->
                DeficitGuardrail(
                    maintenanceKcal = normalizedMaintenance,
                    deficitKcal = deficit,
                    deficitPercent = roundedPercent,
                    level = DeficitRiskLevel.Safe,
                    title = "결핍 강도 안전 구간",
                    message = "유지 대비 ${deficit}kcal(${roundedPercent}%) 결핍으로 안정적인 범위에 가깝습니다.",
                    canSave = true,
                )
        }
    }

    fun recoveryRiskAssessment(
        checks: List<DailyConditionCheck>,
    ): RecoveryRiskAssessment {
        val recentChecks = checks.sortedByDescending { it.date }.take(3)
        if (recentChecks.size < 2) return RecoveryRiskAssessment()

        val dayRiskScores =
            recentChecks.map { check ->
                var score = 0
                if ((check.sleepHours ?: 99f) in 0f..5.99f) score += 1
                if ((check.fatigueScore ?: 0) >= 4) score += 1
                if ((check.hungerScore ?: 0) >= 4) score += 1
                if ((check.moodScore ?: 6) <= 2) score += 1
                if ((check.workoutPerformanceScore ?: 6) <= 2) score += 1
                score
            }

        val flaggedDays = dayRiskScores.count { it > 0 }
        val highDays = dayRiskScores.count { it >= 2 }
        return when {
            highDays >= 2 ->
                RecoveryRiskAssessment(
                    status = RecoveryRiskStatus.High,
                    flaggedDays = flaggedDays,
                    message = "최근 3일 회복 레드플래그가 반복돼요. 목표 칼로리 완화 또는 3~7일 다이어트 브레이크를 권장합니다.",
                    suggestDietBreak = true,
                )

            flaggedDays >= 2 ->
                RecoveryRiskAssessment(
                    status = RecoveryRiskStatus.Watch,
                    flaggedDays = flaggedDays,
                    message = "회복 신호가 누적되고 있어요. 수면·훈련 강도를 점검하고 감량 강도를 미세 완화해보세요.",
                    suggestDietBreak = false,
                )

            else ->
                RecoveryRiskAssessment(
                    status = RecoveryRiskStatus.Stable,
                    flaggedDays = flaggedDays,
                    message = "최근 회복 신호는 안정적이에요. 현재 루틴을 유지하세요.",
                    suggestDietBreak = false,
                )
        }
    }

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

    fun strengthTrend(
        checks: List<DailyConditionCheck>,
    ): StrengthTrend {
        val records = checks.filter { (it.mainLiftKg ?: 0f) > 0f }.sortedBy { it.date }
        if (records.size < 2) return StrengthTrend()

        val first = records.first().mainLiftKg ?: return StrengthTrend()
        val last = records.last().mainLiftKg ?: return StrengthTrend()
        if (first <= 0f) return StrengthTrend()

        val changePercent = ((last - first) / first) * 100f
        val rounded = (changePercent * 10f).roundToInt() / 10f

        return when {
            changePercent >= 2.0f ->
                StrengthTrend(
                    status = StrengthTrendStatus.Up,
                    changePercent = rounded,
                    message = "핵심 리프트가 상승 추세예요 (+${rounded}%). 감량 중 근력 방어가 잘 되고 있습니다.",
                )

            changePercent <= -2.0f ->
                StrengthTrend(
                    status = StrengthTrendStatus.Down,
                    changePercent = rounded,
                    message = "핵심 리프트가 하락 추세예요 (${rounded}%). 회복/브레이크/목표 완화를 우선 점검하세요.",
                )

            else ->
                StrengthTrend(
                    status = StrengthTrendStatus.Stable,
                    changePercent = rounded,
                    message = "핵심 리프트가 안정적이에요 (${rounded}%). 현재 감량 리듬을 유지하세요.",
                )
        }
    }

    fun relapsePreventionInsight(
        checks: List<DailyConditionCheck>,
    ): RelapsePreventionInsight {
        val triggers =
            checks.asSequence()
                .mapNotNull { it.relapseTrigger?.trim()?.takeIf(String::isNotEmpty) }
                .groupingBy { it }
                .eachCount()

        if (triggers.isEmpty()) return RelapsePreventionInsight()

        val recurring = triggers.maxByOrNull { it.value } ?: return RelapsePreventionInsight()
        val action = RelapsePreventionCatalog.recommendedActionFor(recurring.key)
        val message =
            if (recurring.value >= 2) {
                "최근 반복 트리거는 '${recurring.key}' (${recurring.value}회)예요. 대응 루틴을 먼저 고정하면 폭식/이탈을 줄이기 쉽습니다."
            } else {
                "가장 최근 기록된 트리거는 '${recurring.key}'예요. 같은 상황이 오기 전에 대응 루틴을 준비해보세요."
            }

        return RelapsePreventionInsight(
            recurringTrigger = recurring.key,
            recommendedAction = action,
            triggerCount = recurring.value,
            message = message,
        )
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

    fun recoveryAwareCalorieAdjustmentRecommendation(
        currentTargetKcal: Int,
        weeklyWeightTrend: WeeklyWeightTrend,
        recoveryRisk: RecoveryRiskAssessment,
    ): CalorieAdjustmentRecommendation {
        val baseline =
            calorieAdjustmentRecommendation(
                currentTargetKcal = currentTargetKcal,
                weeklyWeightTrend = weeklyWeightTrend,
            )
        if (recoveryRisk.status != RecoveryRiskStatus.High) {
            return baseline
        }

        val targetOptions = TARGET_OPTIONS_KCAL.sorted()
        val higherOption = targetOptions.firstOrNull { it > baseline.currentTargetKcal } ?: baseline.currentTargetKcal
        val delta = (higherOption - baseline.currentTargetKcal).coerceAtLeast(0)
        return if (delta > 0) {
            baseline.copy(
                suggestedTargetKcal = higherOption,
                direction = CalorieAdjustmentDirection.Increase,
                deltaKcal = delta,
                title = "회복 레드플래그: 목표 완화 권고",
                message = "최근 회복 신호가 누적되어 ${delta}kcal 상향을 우선 권장합니다. 필요하면 3~7일 다이어트 브레이크를 진행하세요.",
                actionable = true,
            )
        } else {
            baseline.copy(
                direction = CalorieAdjustmentDirection.Keep,
                title = "회복 우선 구간",
                message = "이미 상단 목표 구간입니다. 이번 주는 추가 하향 없이 회복(수면/피로 관리)을 우선하세요.",
                actionable = false,
            )
        }
    }

    fun leanMassProtectionScore(
        checks: List<DailyConditionCheck>,
        recommendedProteinGrams: Int?,
        recoveryRisk: RecoveryRiskAssessment,
    ): LeanMassProtectionScore {
        val recent = checks.sortedByDescending { it.date }.take(7)
        if (recent.size < 2) return LeanMassProtectionScore()

        val proteinHitDays =
            recent.count { check ->
                val protein = check.proteinGrams ?: 0
                val recommended = recommendedProteinGrams ?: 0
                recommended > 0 && protein >= recommended
            }
        val resistanceHitDays = recent.count { (it.resistanceSets ?: 0) >= 8 }
        val checkinDays = recent.count { check ->
            (check.proteinGrams ?: 0) > 0 || (check.resistanceSets ?: 0) > 0 || (check.bodyWeightKg ?: 0f) > 0f
        }

        val proteinScore = (proteinHitDays * 10).coerceAtMost(50)
        val resistanceScore = (resistanceHitDays * 10).coerceAtMost(30)
        val consistencyScore = (checkinDays * 3).coerceAtMost(20)
        val penalty =
            when (recoveryRisk.status) {
                RecoveryRiskStatus.High -> 20
                RecoveryRiskStatus.Watch -> 10
                else -> 0
            }
        val score = (proteinScore + resistanceScore + consistencyScore - penalty).coerceIn(0, 100)

        val grade =
            when {
                score >= 80 -> LeanMassProtectionGrade.Excellent
                score >= 65 -> LeanMassProtectionGrade.Good
                score >= 45 -> LeanMassProtectionGrade.Moderate
                else -> LeanMassProtectionGrade.Low
            }
        val message =
            when (grade) {
                LeanMassProtectionGrade.Excellent -> "근손실 방어 루틴이 매우 안정적이에요. 현재 패턴을 유지하세요."
                LeanMassProtectionGrade.Good -> "근손실 방어가 잘 되고 있어요. 단백질/훈련 한두 날만 더 보강하면 더 좋아집니다."
                LeanMassProtectionGrade.Moderate -> "기본 루틴은 유지 중이지만 단백질 또는 훈련 달성률 보강이 필요해요."
                LeanMassProtectionGrade.Low -> "근손실 방어 지표가 약해요. 단백질·저항운동·회복 신호를 우선 개선하세요."
                LeanMassProtectionGrade.NoData -> "단백질/훈련 체크가 쌓이면 근손실 방어 점수를 계산해요."
            }

        return LeanMassProtectionScore(
            score = score,
            grade = grade,
            message = message,
            proteinHitDays = proteinHitDays,
            resistanceHitDays = resistanceHitDays,
        )
    }

    fun dietBreakRecommendation(
        phase: MiniCutPhase?,
        recoveryRisk: RecoveryRiskAssessment,
        weeklyWeightTrend: WeeklyWeightTrend,
    ): DietBreakRecommendation {
        if (phase != MiniCutPhase.Active) return DietBreakRecommendation()

        if (recoveryRisk.status == RecoveryRiskStatus.High) {
            return DietBreakRecommendation(
                shouldSuggest = true,
                suggestedDays = 5,
                title = "5일 다이어트 브레이크 권장",
                message = "회복 레드플래그가 누적되었습니다. 3~7일 유지칼로리 구간으로 전환 후 감량을 재개하세요.",
            )
        }

        if (weeklyWeightTrend.status == WeeklyWeightTrendStatus.TooFast && recoveryRisk.status == RecoveryRiskStatus.Watch) {
            return DietBreakRecommendation(
                shouldSuggest = true,
                suggestedDays = 3,
                title = "3일 미니 브레이크 권장",
                message = "감량 속도 과속 + 회복 경고가 함께 보여 짧은 유지구간으로 피로를 완화하는 것이 좋습니다.",
            )
        }

        return DietBreakRecommendation(
            shouldSuggest = false,
            suggestedDays = 0,
            title = "브레이크 불필요",
            message = "현재는 감량 리듬이 안정적입니다. 체크인을 유지하며 진행하세요.",
        )
    }
}
