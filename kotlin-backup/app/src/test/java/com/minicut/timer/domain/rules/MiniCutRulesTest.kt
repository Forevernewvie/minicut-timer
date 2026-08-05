package com.minicut.timer.domain.rules

import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.CalendarRhythmStatus
import com.minicut.timer.domain.model.CalorieAdjustmentDirection
import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.DeficitRiskLevel
import com.minicut.timer.domain.model.LeanMassProtectionGrade
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.MiniCutPlan
import com.minicut.timer.domain.model.MissionType
import com.minicut.timer.domain.model.RecoveryRiskAssessment
import com.minicut.timer.domain.model.RecoveryRiskStatus
import com.minicut.timer.domain.model.StrengthTrendStatus
import com.minicut.timer.domain.model.TargetGuidanceTone
import com.minicut.timer.domain.model.WeeklyWeightTrend
import com.minicut.timer.domain.model.WeeklyWeightTrendStatus
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun reverseDietPlan_increasesTargetsStepwiseByGoalMode() {
        val massReset = MiniCutRules.reverseDietPlan(dailyTargetKcal = 1300, goalMode = MiniCutGoalMode.MassReset)
        val eventReady = MiniCutRules.reverseDietPlan(dailyTargetKcal = 1300, goalMode = MiniCutGoalMode.EventReady)

        assertEquals(3, massReset.steps.size)
        assertEquals(1420, massReset.steps.first().targetCalories)
        assertEquals(1450, eventReady.steps.first().targetCalories)
        assertTrue(eventReady.steps.last().targetCalories > massReset.steps.last().targetCalories)
    }

    @Test
    fun weeklyWeightTrend_classifiesSpeedBand() {
        val checks =
            listOf(
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 1),
                    bodyWeightKg = 80f,
                    updatedAt = LocalDateTime.of(2026, 4, 1, 9, 0),
                ),
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 8),
                    bodyWeightKg = 79f,
                    updatedAt = LocalDateTime.of(2026, 4, 8, 9, 0),
                ),
            )

        val trend = MiniCutRules.weeklyWeightTrend(checks)

        assertEquals(WeeklyWeightTrendStatus.InRange, trend.status)
        assertEquals(1.25f, trend.ratePercentPerWeek)
    }

    @Test
    fun recommendedProteinGrams_matchesWeightTimesTwoRule() {
        assertEquals(160, MiniCutRules.recommendedProteinGrams(80f))
        assertEquals(null, MiniCutRules.recommendedProteinGrams(null))
    }

    @Test
    fun calorieAdjustmentRecommendation_suggestsLowerTargetWhenTrendIsSlow() {
        val recommendation =
            MiniCutRules.calorieAdjustmentRecommendation(
                currentTargetKcal = 1300,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.TooSlow,
                        ratePercentPerWeek = 0.4f,
                    ),
            )

        assertEquals(CalorieAdjustmentDirection.Decrease, recommendation.direction)
        assertEquals(1200, recommendation.suggestedTargetKcal)
        assertEquals(true, recommendation.actionable)
    }

    @Test
    fun calorieAdjustmentRecommendation_keepsTargetWhenInRange() {
        val recommendation =
            MiniCutRules.calorieAdjustmentRecommendation(
                currentTargetKcal = 1300,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.InRange,
                        ratePercentPerWeek = 1.0f,
                    ),
            )

        assertEquals(CalorieAdjustmentDirection.Keep, recommendation.direction)
        assertEquals(1300, recommendation.suggestedTargetKcal)
        assertEquals(false, recommendation.actionable)
    }

    @Test
    fun calorieAdjustmentRecommendation_suggestsHigherTargetWhenTrendTooFast() {
        val recommendation =
            MiniCutRules.calorieAdjustmentRecommendation(
                currentTargetKcal = 1300,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.TooFast,
                        ratePercentPerWeek = 1.8f,
                    ),
            )

        assertEquals(CalorieAdjustmentDirection.Increase, recommendation.direction)
        assertEquals(1400, recommendation.suggestedTargetKcal)
        assertEquals(true, recommendation.actionable)
    }

    @Test
    fun calorieAdjustmentRecommendation_respectsLowerBoundaryOption() {
        val recommendation =
            MiniCutRules.calorieAdjustmentRecommendation(
                currentTargetKcal = 1000,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.GainOrStall,
                        ratePercentPerWeek = -0.2f,
                    ),
            )

        assertEquals(CalorieAdjustmentDirection.Keep, recommendation.direction)
        assertEquals(1000, recommendation.suggestedTargetKcal)
        assertEquals(false, recommendation.actionable)
    }

    @Test
    fun calorieAdjustmentRecommendation_decreasesForGainOrStall() {
        val recommendation =
            MiniCutRules.calorieAdjustmentRecommendation(
                currentTargetKcal = 1300,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.GainOrStall,
                        ratePercentPerWeek = -0.2f,
                    ),
            )

        assertEquals(CalorieAdjustmentDirection.Decrease, recommendation.direction)
        assertEquals(1200, recommendation.suggestedTargetKcal)
        assertEquals(true, recommendation.actionable)
    }

    @Test
    fun calorieAdjustmentRecommendation_respectsUpperBoundaryOption() {
        val recommendation =
            MiniCutRules.calorieAdjustmentRecommendation(
                currentTargetKcal = 1500,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.TooFast,
                        ratePercentPerWeek = 1.9f,
                    ),
            )

        assertEquals(CalorieAdjustmentDirection.Keep, recommendation.direction)
        assertEquals(1500, recommendation.suggestedTargetKcal)
        assertEquals(false, recommendation.actionable)
    }

    @Test
    fun estimateMaintenanceCalories_andDeficitGuardrail_classifyRiskBands() {
        val maintenance = MiniCutRules.estimateMaintenanceCalories(bodyWeightKg = 80f, activityLevel = ActivityLevel.Moderate)
        assertEquals(2480, maintenance)

        val safe = MiniCutRules.deficitGuardrail(targetKcal = 1800, maintenanceKcal = maintenance)
        assertEquals(DeficitRiskLevel.Caution, safe.level)
        assertTrue(safe.canSave)

        val high = MiniCutRules.deficitGuardrail(targetKcal = 1200, maintenanceKcal = maintenance)
        assertEquals(DeficitRiskLevel.High, high.level)
        assertFalse(high.canSave)
    }

    @Test
    fun recoveryRiskAssessment_marksHighWhenSignalsAccumulate() {
        val checks =
            listOf(
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 8),
                    sleepHours = 5.2f,
                    fatigueScore = 4,
                    hungerScore = 4,
                    moodScore = 2,
                    workoutPerformanceScore = 2,
                    updatedAt = LocalDateTime.of(2026, 4, 8, 9, 0),
                ),
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 9),
                    sleepHours = 5.5f,
                    fatigueScore = 4,
                    hungerScore = 4,
                    moodScore = 2,
                    workoutPerformanceScore = 2,
                    updatedAt = LocalDateTime.of(2026, 4, 9, 9, 0),
                ),
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 10),
                    sleepHours = 6.0f,
                    fatigueScore = 4,
                    hungerScore = 4,
                    moodScore = 2,
                    workoutPerformanceScore = 2,
                    updatedAt = LocalDateTime.of(2026, 4, 10, 9, 0),
                ),
            )

        val assessment = MiniCutRules.recoveryRiskAssessment(checks)
        assertEquals(RecoveryRiskStatus.High, assessment.status)
        assertTrue(assessment.suggestDietBreak)
    }

    @Test
    fun recoveryAwareRecommendation_overridesToIncreaseOnHighRecoveryRisk() {
        val recommendation =
            MiniCutRules.recoveryAwareCalorieAdjustmentRecommendation(
                currentTargetKcal = 1300,
                weeklyWeightTrend =
                    WeeklyWeightTrend(
                        status = WeeklyWeightTrendStatus.TooSlow,
                        ratePercentPerWeek = 0.3f,
                    ),
                recoveryRisk = RecoveryRiskAssessment(status = RecoveryRiskStatus.High, flaggedDays = 3, suggestDietBreak = true),
            )

        assertEquals(CalorieAdjustmentDirection.Increase, recommendation.direction)
        assertEquals(1400, recommendation.suggestedTargetKcal)
        assertTrue(recommendation.actionable)
    }

    @Test
    fun leanMassProtectionScore_reflectsProteinResistanceAndRecoveryPenalty() {
        val checks =
            listOf(
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 8),
                    proteinGrams = 170,
                    resistanceSets = 10,
                    updatedAt = LocalDateTime.of(2026, 4, 8, 9, 0),
                ),
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 9),
                    proteinGrams = 165,
                    resistanceSets = 9,
                    updatedAt = LocalDateTime.of(2026, 4, 9, 9, 0),
                ),
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 10),
                    proteinGrams = 160,
                    resistanceSets = 8,
                    updatedAt = LocalDateTime.of(2026, 4, 10, 9, 0),
                ),
            )

        val score =
            MiniCutRules.leanMassProtectionScore(
                checks = checks,
                recommendedProteinGrams = 160,
                recoveryRisk = RecoveryRiskAssessment(status = RecoveryRiskStatus.Stable),
            )

        assertTrue(score.score > 0)
        assertEquals(3, score.proteinHitDays)
        assertEquals(3, score.resistanceHitDays)
        assertTrue(score.grade == LeanMassProtectionGrade.Moderate || score.grade == LeanMassProtectionGrade.Good || score.grade == LeanMassProtectionGrade.Excellent)
    }

    @Test
    fun dietBreakRecommendation_suggestsBreakWhenRecoveryRiskHighDuringActivePhase() {
        val recommendation =
            MiniCutRules.dietBreakRecommendation(
                phase = MiniCutPhase.Active,
                recoveryRisk = RecoveryRiskAssessment(status = RecoveryRiskStatus.High, flaggedDays = 3, suggestDietBreak = true),
                weeklyWeightTrend = WeeklyWeightTrend(status = WeeklyWeightTrendStatus.TooSlow, ratePercentPerWeek = 0.3f),
            )

        assertTrue(recommendation.shouldSuggest)
        assertEquals(5, recommendation.suggestedDays)
    }

    @Test
    fun strengthTrend_reportsUpWhenMainLiftImproves() {
        val trend =
            MiniCutRules.strengthTrend(
                listOf(
                    DailyConditionCheck(
                        date = LocalDate.of(2026, 4, 1),
                        mainLiftKg = 100f,
                        updatedAt = LocalDateTime.of(2026, 4, 1, 9, 0),
                    ),
                    DailyConditionCheck(
                        date = LocalDate.of(2026, 4, 8),
                        mainLiftKg = 104f,
                        updatedAt = LocalDateTime.of(2026, 4, 8, 9, 0),
                    ),
                ),
            )

        assertEquals(StrengthTrendStatus.Up, trend.status)
        assertTrue((trend.changePercent ?: 0f) > 0f)
    }

    @Test
    fun relapsePreventionInsight_picksMostFrequentTrigger() {
        val insight =
            MiniCutRules.relapsePreventionInsight(
                listOf(
                    DailyConditionCheck(
                        date = LocalDate.of(2026, 4, 8),
                        relapseTrigger = "야식",
                        updatedAt = LocalDateTime.of(2026, 4, 8, 9, 0),
                    ),
                    DailyConditionCheck(
                        date = LocalDate.of(2026, 4, 9),
                        relapseTrigger = "스트레스",
                        updatedAt = LocalDateTime.of(2026, 4, 9, 9, 0),
                    ),
                    DailyConditionCheck(
                        date = LocalDate.of(2026, 4, 10),
                        relapseTrigger = "야식",
                        updatedAt = LocalDateTime.of(2026, 4, 10, 9, 0),
                    ),
                ),
            )

        assertEquals("야식", insight.recurringTrigger)
        assertEquals(2, insight.triggerCount)
        assertTrue(insight.recommendedAction?.contains("양치") == true)
    }

    @Test
    fun planProgressSnapshot_exposesDdayAndSupportingCopy() {
        val plan =
            MiniCutPlan(
                startDate = LocalDate.of(2026, 4, 10),
                durationWeeks = 2,
                endDate = LocalDate.of(2026, 4, 23),
                dailyTargetKcal = 1300,
            )

        val snapshot = MiniCutRules.planProgressSnapshot(plan, LocalDate.of(2026, 4, 16))

        assertEquals(MiniCutPhase.Active, snapshot.phase)
        assertEquals("D-8", snapshot.dDayLabel)
        assertEquals(7, snapshot.elapsedDays)
        assertEquals(8, snapshot.remainingDays)
        assertTrue(snapshot.headline.contains("2주"))
    }

    @Test
    fun planProgressSnapshot_labelsEndDateAsDday() {
        val plan =
            MiniCutPlan(
                startDate = LocalDate.of(2026, 4, 10),
                durationWeeks = 2,
                endDate = LocalDate.of(2026, 4, 23),
                dailyTargetKcal = 1300,
            )

        val snapshot = MiniCutRules.planProgressSnapshot(plan, LocalDate.of(2026, 4, 23))

        assertEquals(MiniCutPhase.Active, snapshot.phase)
        assertEquals("D-day", snapshot.dDayLabel)
        assertEquals(1, snapshot.remainingDays)
    }

    @Test
    fun todayMissions_markCompletedHabitsAndWeeklyReview() {
        val missions =
            MiniCutRules.todayMissions(
                hasFoodLog = true,
                hasCoachCheckIn = false,
                weeklyReport =
                    MiniCutRules.weeklyAdherenceReport(
                        summaries =
                            listOf(
                                DailyCalorieSummary(LocalDate.of(2026, 4, 8), 1200, 1),
                                DailyCalorieSummary(LocalDate.of(2026, 4, 9), 1250, 1),
                                DailyCalorieSummary(LocalDate.of(2026, 4, 10), 1300, 1),
                            ),
                        targetCalories = 1300,
                    ),
            )

        assertTrue(missions.first { it.type == MissionType.FoodLog }.isComplete)
        assertFalse(missions.first { it.type == MissionType.CoachCheckIn }.isComplete)
        assertTrue(missions.first { it.type == MissionType.WeeklyReview }.isComplete)
    }

    @Test
    fun calendarRhythmSummary_countsFoodAndCheckInSignals() {
        val summaries =
            listOf(
                DailyCalorieSummary(LocalDate.of(2026, 4, 1), 1200, 1),
                DailyCalorieSummary(LocalDate.of(2026, 4, 2), 1600, 1),
                DailyCalorieSummary(LocalDate.of(2026, 4, 3), 0, 0),
            )
        val checks =
            listOf(
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 1),
                    fatigueScore = 2,
                    updatedAt = LocalDateTime.of(2026, 4, 1, 9, 0),
                ),
                DailyConditionCheck(
                    date = LocalDate.of(2026, 4, 3),
                    hungerScore = 3,
                    updatedAt = LocalDateTime.of(2026, 4, 3, 9, 0),
                ),
            )

        val summary = MiniCutRules.calendarRhythmSummary(summaries, checks, targetCalories = 1300)

        assertEquals(2, summary.loggedDays)
        assertEquals(1, summary.withinTargetDays)
        assertEquals(1, summary.overTargetDays)
        assertEquals(2, summary.checkInDays)
        assertEquals(CalendarRhythmStatus.WithinTarget, MiniCutRules.calendarRhythmStatus(1200, 1300))
        assertEquals(CalendarRhythmStatus.OverTarget, MiniCutRules.calendarRhythmStatus(1600, 1300))
    }

    @Test
    fun stateAwareReminderMessage_prioritizesRecoveryRiskOverStaticPrompt() {
        val today = LocalDate.of(2026, 4, 10)
        val checks =
            listOf(
                DailyConditionCheck(
                    date = today.minusDays(1),
                    sleepHours = 5f,
                    fatigueScore = 5,
                    hungerScore = 5,
                    updatedAt = LocalDateTime.of(2026, 4, 9, 9, 0),
                ),
                DailyConditionCheck(
                    date = today,
                    sleepHours = 5f,
                    fatigueScore = 5,
                    hungerScore = 5,
                    updatedAt = LocalDateTime.of(2026, 4, 10, 9, 0),
                ),
            )

        val message =
            MiniCutRules.stateAwareReminderMessage(
                isEvening = true,
                currentDate = today,
                recentSummaries = emptyList(),
                recentChecks = checks,
            )

        assertTrue(message.contains("회복"))
        assertTrue(message.contains("체크인"))
    }
}
