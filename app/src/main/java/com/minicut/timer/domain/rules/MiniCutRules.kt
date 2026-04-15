package com.minicut.timer.domain.rules

import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.TargetGuidance
import com.minicut.timer.domain.model.TargetGuidanceTone
import com.minicut.timer.domain.model.WeeklyAdherenceReport
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
}
