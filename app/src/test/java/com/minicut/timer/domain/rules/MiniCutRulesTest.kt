package com.minicut.timer.domain.rules

import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.TargetGuidanceTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniCutRulesTest {

    @Test
    fun duration_andTargetBoundaries_followMiniCutPolicy() {
        assertFalse(MiniCutRules.isValidDuration(1))
        assertTrue(MiniCutRules.isValidDuration(2))
        assertTrue(MiniCutRules.isValidDuration(6))
        assertFalse(MiniCutRules.isValidDuration(7))

        assertFalse(MiniCutRules.isValidTarget(999))
        assertTrue(MiniCutRules.isValidTarget(1000))
        assertTrue(MiniCutRules.isValidTarget(1500))
        assertFalse(MiniCutRules.isValidTarget(1501))
    }

    @Test
    fun calculateEndDate_returnsInclusiveEndDay() {
        val start = LocalDate.of(2026, 4, 10)

        val end = MiniCutRules.calculateEndDate(startDate = start, durationWeeks = 4)

        assertEquals(LocalDate.of(2026, 5, 7), end)
    }

    @Test(expected = IllegalArgumentException::class)
    fun calculateEndDate_rejectsWeeksOutsideRange() {
        MiniCutRules.calculateEndDate(
            startDate = LocalDate.of(2026, 4, 10),
            durationWeeks = 7,
        )
    }

    @Test
    fun calculateProgress_andRemainingDays_coverActivePlan() {
        val start = LocalDate.of(2026, 4, 10)
        val end = MiniCutRules.calculateEndDate(start, 2)
        val today = LocalDate.of(2026, 4, 16)

        assertEquals(8, MiniCutRules.remainingDays(start, end, today))
        assertEquals(0.5f, MiniCutRules.calculateProgress(start, end, today), 0.001f)
        assertTrue(MiniCutRules.isDateInsidePlan(today, start, end))
        assertFalse(MiniCutRules.isDateInsidePlan(end.plusDays(1), start, end))
    }

    @Test
    fun calculateProgress_andRemainingDays_handleBeforeAndAfterPlanBoundaries() {
        val start = LocalDate.of(2026, 4, 10)
        val end = MiniCutRules.calculateEndDate(start, 2)

        assertEquals(0f, MiniCutRules.calculateProgress(start, end, start.minusDays(1)), 0.001f)
        assertEquals(1f, MiniCutRules.calculateProgress(start, end, end.plusDays(1)), 0.001f)
        assertEquals(14, MiniCutRules.remainingDays(start, end, start.minusDays(3)))
        assertEquals(0, MiniCutRules.remainingDays(start, end, end.plusDays(2)))
    }

    @Test
    fun rangeStatus_classifiesCalorieBands() {
        assertEquals(CalorieRangeStatus.NoData, MiniCutRules.rangeStatus(null))
        assertEquals(CalorieRangeStatus.NoData, MiniCutRules.rangeStatus(0))
        assertEquals(CalorieRangeStatus.Below, MiniCutRules.rangeStatus(950))
        assertEquals(CalorieRangeStatus.InRange, MiniCutRules.rangeStatus(1200))
        assertEquals(CalorieRangeStatus.Above, MiniCutRules.rangeStatus(1650))
    }

    @Test
    fun target_validation_and_budget_math_coverRemainingAndOverStates() {
        assertTrue(MiniCutRules.isValidTarget(1300))
        assertFalse(MiniCutRules.isValidTarget(900))
        assertEquals(250, MiniCutRules.remainingCalories(targetCalories = 1300, consumedCalories = 1050))
        assertEquals(180, MiniCutRules.overCalories(targetCalories = 1300, consumedCalories = 1480))
        assertTrue(MiniCutRules.isOverTarget(targetCalories = 1300, consumedCalories = 1480))
        assertFalse(MiniCutRules.isOverTarget(targetCalories = 1300, consumedCalories = 1050))
        assertEquals(CalorieRangeStatus.Below, MiniCutRules.targetStatus(totalCalories = 1050, targetCalories = 1300))
        assertEquals(CalorieRangeStatus.InRange, MiniCutRules.targetStatus(totalCalories = 1300, targetCalories = 1300))
        assertEquals(CalorieRangeStatus.Above, MiniCutRules.targetStatus(totalCalories = 1480, targetCalories = 1300))
        assertEquals(CalorieRangeStatus.NoData, MiniCutRules.targetStatus(totalCalories = null, targetCalories = 1300))
        assertEquals(CalorieRangeStatus.NoData, MiniCutRules.targetStatus(totalCalories = 0, targetCalories = 1300))
    }

    @Test
    fun phaseOf_distinguishesUpcomingActiveAndCompletedPlans() {
        val start = LocalDate.of(2026, 4, 10)
        val end = MiniCutRules.calculateEndDate(start, 2)

        assertEquals(MiniCutPhase.Upcoming, MiniCutRules.phaseOf(start, end, LocalDate.of(2026, 4, 9)))
        assertEquals(MiniCutPhase.Active, MiniCutRules.phaseOf(start, end, LocalDate.of(2026, 4, 10)))
        assertEquals(MiniCutPhase.Completed, MiniCutRules.phaseOf(start, end, end.plusDays(1)))
    }

    @Test
    fun weeklyAdherenceReport_summarizesSevenDayFlow() {
        val week =
            listOf(
                DailyCalorieSummary(LocalDate.of(2026, 4, 4), totalCalories = 1200, entryCount = 2),
                DailyCalorieSummary(LocalDate.of(2026, 4, 5), totalCalories = 0, entryCount = 0),
                DailyCalorieSummary(LocalDate.of(2026, 4, 6), totalCalories = 1450, entryCount = 3),
                DailyCalorieSummary(LocalDate.of(2026, 4, 7), totalCalories = 1320, entryCount = 2),
            )

        val report = MiniCutRules.weeklyAdherenceReport(week, targetCalories = 1300)

        assertEquals(3, report.loggedDays)
        assertEquals(1, report.adherentDays)
        assertEquals(2, report.overTargetDays)
        assertEquals((1200 + 1450 + 1320) / 3, report.averageLoggedCalories)
    }

    @Test
    fun targetGuidance_changesToneByTargetAndDuration() {
        val caution = MiniCutRules.targetGuidance(targetCalories = 1100, durationWeeks = 6)
        val recommended = MiniCutRules.targetGuidance(targetCalories = 1300, durationWeeks = 4)
        val flexible = MiniCutRules.targetGuidance(targetCalories = 1500, durationWeeks = 3)

        assertEquals(TargetGuidanceTone.Caution, caution.tone)
        assertEquals(TargetGuidanceTone.Recommended, recommended.tone)
        assertEquals(TargetGuidanceTone.Flexible, flexible.tone)
    }
}
