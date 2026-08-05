import { MiniCutRules, addDays, parseLocalDate, toLocalDateString } from "./rules";
import {
  MiniCutPlan,
  DailyCalorieSummary,
  DailyConditionCheck,
  WeeklyWeightTrend,
  RecoveryRiskAssessment,
} from "./models";

function assert(condition: boolean, message: string) {
  if (!condition) {
    throw new Error(`Assertion failed: ${message}`);
  }
}

function assertEquals<T>(expected: T, actual: T, message: string) {
  if (expected !== actual) {
    throw new Error(`Assertion failed: ${message}\nExpected: ${expected}\nActual:   ${actual}`);
  }
}

function runTests() {
  console.log("🏃 Running MiniCutRules tests...");

  // 1. duration_andTargetBoundaries_followMiniCutPolicy
  assert(!MiniCutRules.isValidDuration(1), "isValidDuration(1) should be false");
  assert(MiniCutRules.isValidDuration(2), "isValidDuration(2) should be true");
  assert(MiniCutRules.isValidDuration(6), "isValidDuration(6) should be true");
  assert(!MiniCutRules.isValidDuration(7), "isValidDuration(7) should be false");

  assert(!MiniCutRules.isValidTarget(999), "isValidTarget(999) should be false");
  assert(MiniCutRules.isValidTarget(1000), "isValidTarget(1000) should be true");
  assert(MiniCutRules.isValidTarget(1500), "isValidTarget(1500) should be true");
  assert(!MiniCutRules.isValidTarget(1501), "isValidTarget(1501) should be false");
  console.log("✅ duration_andTargetBoundaries_followMiniCutPolicy passed");

  // 2. calculateEndDate_returnsInclusiveEndDay
  const start = "2026-04-10";
  const end = MiniCutRules.calculateEndDate(start, 4);
  assertEquals("2026-05-07", end, "calculateEndDate(4 weeks) should be 2026-05-07");
  console.log("✅ calculateEndDate_returnsInclusiveEndDay passed");

  // 3. calculateProgress_andRemainingDays_coverActivePlan
  const start2 = "2026-04-10";
  const end2 = MiniCutRules.calculateEndDate(start2, 2); // 2026-04-23
  const today = "2026-04-16";

  assertEquals(8, MiniCutRules.remainingDays(start2, end2, today), "remainingDays should be 8");
  assertEquals(0.5, MiniCutRules.calculateProgress(start2, end2, today), "calculateProgress should be 0.5");
  assert(MiniCutRules.isDateInsidePlan(today, start2, end2), "today is inside plan");
  assert(!MiniCutRules.isDateInsidePlan(addDays(end2, 1), start2, end2), "day after end is not inside plan");
  console.log("✅ calculateProgress_andRemainingDays_coverActivePlan passed");

  // 4. calculateProgress_andRemainingDays_handleBeforeAndAfterPlanBoundaries
  assertEquals(0, MiniCutRules.calculateProgress(start2, end2, addDays(start2, -1)), "progress before plan should be 0");
  assertEquals(1, MiniCutRules.calculateProgress(start2, end2, addDays(end2, 1)), "progress after plan should be 1");
  assertEquals(14, MiniCutRules.remainingDays(start2, end2, addDays(start2, -3)), "remaining days before plan start");
  assertEquals(0, MiniCutRules.remainingDays(start2, end2, addDays(end2, 2)), "remaining days after plan end");
  console.log("✅ calculateProgress_andRemainingDays_handleBeforeAndAfterPlanBoundaries passed");

  // 5. rangeStatus_classifiesCalorieBands
  assertEquals('NoData', MiniCutRules.rangeStatus(null), "rangeStatus(null)");
  assertEquals('NoData', MiniCutRules.rangeStatus(0), "rangeStatus(0)");
  assertEquals('Below', MiniCutRules.rangeStatus(950), "rangeStatus(950)");
  assertEquals('InRange', MiniCutRules.rangeStatus(1200), "rangeStatus(1200)");
  assertEquals('Above', MiniCutRules.rangeStatus(1650), "rangeStatus(1650)");
  console.log("✅ rangeStatus_classifiesCalorieBands passed");

  // 6. target_validation_and_budget_math_coverRemainingAndOverStates
  assert(MiniCutRules.isValidTarget(1300), "isValidTarget(1300)");
  assert(!MiniCutRules.isValidTarget(900), "isValidTarget(900)");
  assertEquals(250, MiniCutRules.remainingCalories(1300, 1050), "remainingCalories");
  assertEquals(180, MiniCutRules.overCalories(1300, 1480), "overCalories");
  assert(MiniCutRules.isOverTarget(1300, 1480), "isOverTarget");
  assert(!MiniCutRules.isOverTarget(1300, 1050), "isOverTarget false");
  assertEquals('Below', MiniCutRules.targetStatus(1050, 1300), "targetStatus Below");
  assertEquals('InRange', MiniCutRules.targetStatus(1300, 1300), "targetStatus InRange");
  assertEquals('Above', MiniCutRules.targetStatus(1480, 1300), "targetStatus Above");
  assertEquals('NoData', MiniCutRules.targetStatus(null, 1300), "targetStatus NoData");
  assertEquals('NoData', MiniCutRules.targetStatus(0, 1300), "targetStatus NoData 0");
  console.log("✅ target_validation_and_budget_math_coverRemainingAndOverStates passed");

  // 7. phaseOf_distinguishesUpcomingActiveAndCompletedPlans
  assertEquals('Upcoming', MiniCutRules.phaseOf(start2, end2, "2026-04-09"), "phaseOf Upcoming");
  assertEquals('Active', MiniCutRules.phaseOf(start2, end2, "2026-04-10"), "phaseOf Active");
  assertEquals('Completed', MiniCutRules.phaseOf(start2, end2, addDays(end2, 1)), "phaseOf Completed");
  console.log("✅ phaseOf_distinguishesUpcomingActiveAndCompletedPlans passed");

  // 8. weeklyAdherenceReport_summarizesSevenDayFlow
  const week: DailyCalorieSummary[] = [
    { date: "2026-04-04", totalCalories: 1200, entryCount: 2 },
    { date: "2026-04-05", totalCalories: 0, entryCount: 0 },
    { date: "2026-04-06", totalCalories: 1450, entryCount: 3 },
    { date: "2026-04-07", totalCalories: 1320, entryCount: 2 },
  ];
  const report = MiniCutRules.weeklyAdherenceReport(week, 1300);
  assertEquals(3, report.loggedDays, "report.loggedDays");
  assertEquals(1, report.adherentDays, "report.adherentDays");
  assertEquals(2, report.overTargetDays, "report.overTargetDays");
  assertEquals(Math.round((1200 + 1450 + 1320) / 3), report.averageLoggedCalories, "report.averageLoggedCalories");
  console.log("✅ weeklyAdherenceReport_summarizesSevenDayFlow passed");

  // 9. targetGuidance_changesToneByTargetAndDuration
  const caution = MiniCutRules.targetGuidance(1100, 6);
  const recommended = MiniCutRules.targetGuidance(1300, 4);
  const flexible = MiniCutRules.targetGuidance(1500, 3);
  assertEquals('Caution', caution.tone, "caution tone");
  assertEquals('Recommended', recommended.tone, "recommended tone");
  assertEquals('Flexible', flexible.tone, "flexible tone");
  console.log("✅ targetGuidance_changesToneByTargetAndDuration passed");

  // 10. reverseDietPlan_increasesTargetsStepwiseByGoalMode
  const massReset = MiniCutRules.reverseDietPlan(1300, 'MassReset');
  const eventReady = MiniCutRules.reverseDietPlan(1300, 'EventReady');
  assertEquals(3, massReset.steps.length, "massReset steps");
  assertEquals(1420, massReset.steps[0].targetCalories, "massReset step 1");
  assertEquals(1450, eventReady.steps[0].targetCalories, "eventReady step 1");
  assert(eventReady.steps[2].targetCalories > massReset.steps[2].targetCalories, "eventReady final > massReset final");
  console.log("✅ reverseDietPlan_increasesTargetsStepwiseByGoalMode passed");

  // 11. weeklyWeightTrend_classifiesSpeedBand
  const checks: DailyConditionCheck[] = [
    {
      date: "2026-04-01",
      bodyWeightKg: 80,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      relapseTrigger: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: "2026-04-01T09:00:00Z"
    },
    {
      date: "2026-04-08",
      bodyWeightKg: 79,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      relapseTrigger: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: "2026-04-08T09:00:00Z"
    }
  ];
  const trend = MiniCutRules.weeklyWeightTrend(checks);
  assertEquals('InRange', trend.status, "weeklyWeightTrend status");
  assertEquals(1.25, trend.ratePercentPerWeek, "weeklyWeightTrend rate");
  console.log("✅ weeklyWeightTrend_classifiesSpeedBand passed");

  // 12. recommendedProteinGrams_matchesWeightTimesTwoRule
  assertEquals(160, MiniCutRules.recommendedProteinGrams(80), "recommendedProteinGrams(80)");
  assertEquals(null, MiniCutRules.recommendedProteinGrams(null), "recommendedProteinGrams(null)");
  console.log("✅ recommendedProteinGrams_matchesWeightTimesTwoRule passed");

  // 13. calorieAdjustmentRecommendation_suggestsLowerTargetWhenTrendIsSlow
  const rec1 = MiniCutRules.calorieAdjustmentRecommendation(1300, {
    status: 'TooSlow',
    ratePercentPerWeek: 0.4,
    message: ""
  });
  assertEquals('Decrease', rec1.direction, "rec1 direction");
  assertEquals(1200, rec1.suggestedTargetKcal, "rec1 suggested");
  assertEquals(true, rec1.actionable, "rec1 actionable");
  console.log("✅ calorieAdjustmentRecommendation_suggestsLowerTargetWhenTrendIsSlow passed");

  // 14. calorieAdjustmentRecommendation_keepsTargetWhenInRange
  const rec2 = MiniCutRules.calorieAdjustmentRecommendation(1300, {
    status: 'InRange',
    ratePercentPerWeek: 1.0,
    message: ""
  });
  assertEquals('Keep', rec2.direction, "rec2 direction");
  assertEquals(1300, rec2.suggestedTargetKcal, "rec2 suggested");
  assertEquals(false, rec2.actionable, "rec2 actionable");
  console.log("✅ calorieAdjustmentRecommendation_keepsTargetWhenInRange passed");

  // 15. calorieAdjustmentRecommendation_suggestsHigherTargetWhenTrendTooFast
  const rec3 = MiniCutRules.calorieAdjustmentRecommendation(1300, {
    status: 'TooFast',
    ratePercentPerWeek: 1.8,
    message: ""
  });
  assertEquals('Increase', rec3.direction, "rec3 direction");
  assertEquals(1400, rec3.suggestedTargetKcal, "rec3 suggested");
  assertEquals(true, rec3.actionable, "rec3 actionable");
  console.log("✅ calorieAdjustmentRecommendation_suggestsHigherTargetWhenTrendTooFast passed");

  // 16. calorieAdjustmentRecommendation_respectsLowerBoundaryOption
  const rec4 = MiniCutRules.calorieAdjustmentRecommendation(1000, {
    status: 'GainOrStall',
    ratePercentPerWeek: -0.2,
    message: ""
  });
  assertEquals('Keep', rec4.direction, "rec4 direction");
  assertEquals(1000, rec4.suggestedTargetKcal, "rec4 suggested");
  assertEquals(false, rec4.actionable, "rec4 actionable");
  console.log("✅ calorieAdjustmentRecommendation_respectsLowerBoundaryOption passed");

  // 17. estimateMaintenanceCalories_andDeficitGuardrail_classifyRiskBands
  const maintenance = MiniCutRules.estimateMaintenanceCalories(80, 'Moderate');
  assertEquals(2480, maintenance, "maintenance calculation");

  const guardrailSafe = MiniCutRules.deficitGuardrail(1800, maintenance);
  assertEquals('Caution', guardrailSafe.level, "guardrailSafe level");
  assert(guardrailSafe.canSave, "guardrailSafe canSave");

  const guardrailHigh = MiniCutRules.deficitGuardrail(1200, maintenance);
  assertEquals('High', guardrailHigh.level, "guardrailHigh level");
  assert(!guardrailHigh.canSave, "guardrailHigh canSave");
  console.log("✅ estimateMaintenanceCalories_andDeficitGuardrail_classifyRiskBands passed");

  // 18. recoveryRiskAssessment_marksHighWhenSignalsAccumulate
  const recoveryChecks: DailyConditionCheck[] = [
    {
      date: "2026-04-08",
      sleepHours: 5.2,
      fatigueScore: 4,
      hungerScore: 4,
      moodScore: 2,
      workoutPerformanceScore: 2,
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      relapseTrigger: null,
      copingAction: null,
      updatedAt: "2026-04-08T09:00:00Z"
    },
    {
      date: "2026-04-09",
      sleepHours: 5.5,
      fatigueScore: 4,
      hungerScore: 4,
      moodScore: 2,
      workoutPerformanceScore: 2,
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      relapseTrigger: null,
      copingAction: null,
      updatedAt: "2026-04-09T09:00:00Z"
    },
    {
      date: "2026-04-10",
      sleepHours: 6.0,
      fatigueScore: 4,
      hungerScore: 4,
      moodScore: 2,
      workoutPerformanceScore: 2,
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      relapseTrigger: null,
      copingAction: null,
      updatedAt: "2026-04-10T09:00:00Z"
    }
  ];
  const assessment = MiniCutRules.recoveryRiskAssessment(recoveryChecks);
  assertEquals('High', assessment.status, "recoveryRisk status");
  assert(assessment.suggestDietBreak, "recoveryRisk suggestDietBreak");
  console.log("✅ recoveryRiskAssessment_marksHighWhenSignalsAccumulate passed");

  // 19. recoveryAwareRecommendation_overridesToIncreaseOnHighRecoveryRisk
  const rec5 = MiniCutRules.recoveryAwareCalorieAdjustmentRecommendation(
    1300,
    { status: 'TooSlow', ratePercentPerWeek: 0.3, message: "" },
    { status: 'High', flaggedDays: 3, message: "", suggestDietBreak: true }
  );
  assertEquals('Increase', rec5.direction, "rec5 direction");
  assertEquals(1400, rec5.suggestedTargetKcal, "rec5 suggested");
  assert(rec5.actionable, "rec5 actionable");
  console.log("✅ recoveryAwareRecommendation_overridesToIncreaseOnHighRecoveryRisk passed");

  // 20. dietBreakRecommendation_suggestsBreakWhenRecoveryRiskHighDuringActivePhase
  const dRecommendation = MiniCutRules.dietBreakRecommendation(
    'Active',
    { status: 'High', flaggedDays: 3, message: "", suggestDietBreak: true },
    { status: 'TooSlow', ratePercentPerWeek: 0.3, message: "" }
  );
  assert(dRecommendation.shouldSuggest, "shouldSuggest dietBreak");
  assertEquals(5, dRecommendation.suggestedDays, "suggestedDays dietBreak");
  console.log("✅ dietBreakRecommendation_suggestsBreakWhenRecoveryRiskHighDuringActivePhase passed");

  // 21. strengthTrend_reportsUpWhenMainLiftImproves
  const strengthChecks: DailyConditionCheck[] = [
    {
      date: "2026-04-01",
      mainLiftKg: 100,
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      relapseTrigger: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: ""
    },
    {
      date: "2026-04-08",
      mainLiftKg: 104,
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      relapseTrigger: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: ""
    }
  ];
  const sTrend = MiniCutRules.strengthTrend(strengthChecks);
  assertEquals('Up', sTrend.status, "strengthTrend status");
  assert((sTrend.changePercent || 0) > 0, "strengthTrend changePercent > 0");
  console.log("✅ strengthTrend_reportsUpWhenMainLiftImproves passed");

  // 22. relapsePreventionInsight_picksMostFrequentTrigger
  const relapseChecks: DailyConditionCheck[] = [
    {
      date: "2026-04-08",
      relapseTrigger: "야식",
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: ""
    },
    {
      date: "2026-04-09",
      relapseTrigger: "스트레스",
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: ""
    },
    {
      date: "2026-04-10",
      relapseTrigger: "야식",
      bodyWeightKg: null,
      proteinGrams: null,
      resistanceSets: null,
      mainLiftKg: null,
      copingAction: null,
      sleepHours: null,
      fatigueScore: null,
      hungerScore: null,
      moodScore: null,
      workoutPerformanceScore: null,
      updatedAt: ""
    }
  ];
  const rInsight = MiniCutRules.relapsePreventionInsight(relapseChecks);
  assertEquals("야식", rInsight.recurringTrigger, "recurringTrigger");
  assertEquals(2, rInsight.triggerCount, "triggerCount");
  assert(rInsight.recommendedAction?.includes("양치") === true, "recommendedAction content");
  console.log("✅ relapsePreventionInsight_picksMostFrequentTrigger passed");

  console.log("🎉 All MiniCutRules tests passed successfully!");
}

runTests();
