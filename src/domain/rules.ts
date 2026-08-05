import {
  ActivityLevels,
  ActivityLevelType,
  CalendarRhythmStatus,
  CalendarRhythmSummary,
  CalorieAdjustmentDirection,
  CalorieAdjustmentRecommendation,
  CalorieRangeStatus,
  DailyCalorieSummary,
  DailyConditionCheck,
  DeficitGuardrail,
  DeficitRiskLevel,
  DietBreakRecommendation,
  EntryQuickPreset,
  LeanMassProtectionGrade,
  LeanMassProtectionScore,
  MiniCutGoalModeType,
  MiniCutPhase,
  MiniCutPlan,
  MissionType,
  PlanProgressSnapshot,
  RecoveryRiskAssessment,
  RecoveryRiskStatus,
  RelapsePreventionInsight,
  ReverseDietPlan,
  ReverseDietStep,
  StrengthTrend,
  StrengthTrendStatus,
  TargetGuidance,
  TargetGuidanceTone,
  TodayMission,
  WeeklyAdherenceReport,
  WeeklyCoachingSnapshot,
  WeeklyWeightTrend,
  WeeklyWeightTrendStatus
} from "./models";

// Helper Date Utilities
export function parseLocalDate(dateStr: string): Date {
  const [year, month, day] = dateStr.split('-').map(Number);
  // Using Local Time to construct Date, avoiding timezone offsets
  return new Date(year, month - 1, day);
}

export function toLocalDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function addDays(dateStr: string, days: number): string {
  const date = parseLocalDate(dateStr);
  date.setDate(date.getDate() + days);
  return toLocalDateString(date);
}

export function addWeeks(dateStr: string, weeks: number): string {
  const date = parseLocalDate(dateStr);
  date.setDate(date.getDate() + (weeks * 7));
  return toLocalDateString(date);
}

export function daysBetween(startDateStr: string, endDateStr: string): number {
  const start = parseLocalDate(startDateStr);
  const end = parseLocalDate(endDateStr);
  const diffTime = end.getTime() - start.getTime();
  return Math.round(diffTime / (1000 * 60 * 60 * 24));
}

export const RelapsePreventionCatalog = {
  TRIGGER_LATE_NIGHT: "야식",
  TRIGGER_STRESS: "스트레스",
  TRIGGER_SOCIAL: "회식",
  TRIGGER_SLEEP_DEBT: "수면부족",

  ACTION_WALK: "산책",
  ACTION_BRUSH: "양치",
  ACTION_PROTEIN_SNACK: "단백질 간식",
  ACTION_WATER: "물 500ml",

  triggerOptions: ["야식", "스트레스", "회식", "수면부족"],
  copingActionOptions: ["산책", "양치", "단백질 간식", "물 500ml"],

  recommendedActionFor(trigger: string): string {
    switch (trigger) {
      case "야식":
        return "양치 + 단백질 간식으로 마감 루틴 만들기";
      case "스트레스":
        return "10분 산책 + 물 500ml 후 결정하기";
      case "회식":
        return "식전 단백질 선행 + 첫 접시 고정하기";
      case "수면부족":
        return "오늘은 유지 칼로리 또는 미니 브레이크 우선 고려하기";
      default:
        return "반복 트리거가 오는 시간대에 미리 대체 행동을 정해두기";
    }
  }
};

export const MiniCutRules = {
  MIN_WEEKS: 2,
  MAX_WEEKS: 6,
  RECOMMENDED_MIN_KCAL: 1000,
  RECOMMENDED_MAX_KCAL: 1500,
  TARGET_OPTIONS_KCAL: [1000, 1200, 1300, 1400, 1500],
  DEFAULT_TARGET_KCAL: 1300,
  HIGH_DEFICIT_PERCENT: 35,
  CAUTION_DEFICIT_PERCENT: 25,
  HIGH_DEFICIT_KCAL: 900,
  CAUTION_DEFICIT_KCAL: 700,

  isValidDuration(durationWeeks: number): boolean {
    return durationWeeks >= this.MIN_WEEKS && durationWeeks <= this.MAX_WEEKS;
  },

  isValidTarget(targetKcal: number): boolean {
    return targetKcal >= this.RECOMMENDED_MIN_KCAL && targetKcal <= this.RECOMMENDED_MAX_KCAL;
  },

  calculateEndDate(startDate: string, durationWeeks: number): string {
    if (!this.isValidDuration(durationWeeks)) {
      throw new Error("미니컷 기간은 2~6주만 허용됩니다.");
    }
    // plusWeeks(durationWeeks).minusDays(1)
    const end = parseLocalDate(startDate);
    end.setDate(end.getDate() + (durationWeeks * 7) - 1);
    return toLocalDateString(end);
  },

  calculateProgress(startDate: string, endDate: string, todayStr: string = toLocalDateString(new Date())): number {
    if (todayStr < startDate) return 0;
    if (todayStr > endDate) return 1;
    const totalDays = daysBetween(startDate, endDate) + 1;
    const elapsedDays = daysBetween(startDate, todayStr) + 1;
    const progress = elapsedDays / totalDays;
    return Math.max(0, Math.min(1, progress));
  },

  isDateInsidePlan(date: string, startDate: string, endDate: string): boolean {
    return date >= startDate && date <= endDate;
  },

  remainingDays(startDate: string, endDate: string, todayStr: string = toLocalDateString(new Date())): number {
    if (todayStr < startDate) {
      return daysBetween(startDate, endDate) + 1;
    }
    if (todayStr > endDate) {
      return 0;
    }
    return daysBetween(todayStr, endDate) + 1;
  },

  phaseOf(startDate: string, endDate: string, todayStr: string = toLocalDateString(new Date())): MiniCutPhase {
    if (todayStr < startDate) return 'Upcoming';
    if (todayStr > endDate) return 'Completed';
    return 'Active';
  },

  planProgressSnapshot(plan: MiniCutPlan, currentDate: string): PlanProgressSnapshot {
    const phase = this.phaseOf(plan.startDate, plan.endDate, currentDate);
    const progress = this.calculateProgress(plan.startDate, plan.endDate, currentDate);
    const totalDays = daysBetween(plan.startDate, plan.endDate) + 1;
    const elapsedDays = Math.max(0, Math.min(totalDays, daysBetween(plan.startDate, currentDate) + 1));
    const remDays = this.remainingDays(plan.startDate, plan.endDate, currentDate);
    const daysUntilStart = Math.max(0, daysBetween(currentDate, plan.startDate));

    let dDayLabel = "";
    if (phase === 'Upcoming') {
      dDayLabel = `D-${daysUntilStart}`;
    } else if (phase === 'Active') {
      dDayLabel = currentDate === plan.endDate ? "D-day" : `D-${remDays}`;
    } else {
      dDayLabel = "완료";
    }

    let headline = "";
    if (phase === 'Upcoming') {
      headline = `${daysUntilStart}일 뒤 미니컷 시작`;
    } else if (phase === 'Active') {
      headline = `${plan.durationWeeks}주 플랜 진행 중`;
    } else {
      headline = "미니컷 기간 완료";
    }

    let supportingText = "";
    if (phase === 'Upcoming') {
      supportingText = "시작 전에는 기록 루틴과 하루 기준만 준비하면 충분해요.";
    } else if (phase === 'Active') {
      supportingText = `오늘까지 ${elapsedDays}일째예요. 끝나는 날짜가 정해져 있으니 오늘 행동만 마무리하세요.`;
    } else {
      supportingText = "이제 급하게 더 줄이기보다 유지·리버스 전환을 점검할 시점입니다.";
    }

    return {
      phase,
      progress,
      elapsedDays,
      remainingDays: remDays,
      dDayLabel,
      headline,
      supportingText,
    };
  },

  remainingCalories(targetCalories: number, consumedCalories: number): number {
    return Math.max(0, targetCalories - consumedCalories);
  },

  overCalories(targetCalories: number, consumedCalories: number): number {
    return Math.max(0, consumedCalories - targetCalories);
  },

  isOverTarget(targetCalories: number, consumedCalories: number): boolean {
    return consumedCalories > targetCalories;
  },

  targetStatus(totalCalories: number | null, targetCalories: number): CalorieRangeStatus {
    if (totalCalories === null || totalCalories <= 0) return 'NoData';
    if (totalCalories < targetCalories) return 'Below';
    if (totalCalories === targetCalories) return 'InRange';
    return 'Above';
  },

  rangeStatus(totalCalories: number | null): CalorieRangeStatus {
    if (totalCalories === null || totalCalories <= 0) return 'NoData';
    if (totalCalories < this.RECOMMENDED_MIN_KCAL) return 'Below';
    if (totalCalories <= this.RECOMMENDED_MAX_KCAL) return 'InRange';
    return 'Above';
  },

  weeklyAdherenceReport(summaries: DailyCalorieSummary[], targetCalories: number): WeeklyAdherenceReport {
    const loggedSummaries = summaries.filter(s => s.totalCalories > 0);
    const loggedDays = loggedSummaries.length;
    const adherentDays = loggedSummaries.filter(s => s.totalCalories <= targetCalories).length;
    const overTargetDays = loggedSummaries.filter(s => s.totalCalories > targetCalories).length;
    const averageLoggedCalories = loggedDays === 0 ? 0 : Math.round(loggedSummaries.reduce((sum, s) => sum + s.totalCalories, 0) / loggedDays);

    let focusMessage = "";
    if (loggedDays === 0) {
      focusMessage = "최근 7일 기록이 없어요. 오늘 한 끼만 적어도 유지/복기 흐름이 다시 살아납니다.";
    } else if (adherentDays >= 5 && overTargetDays === 0) {
      focusMessage = "이번 주는 흐름이 안정적이에요. 종료 후에는 유지 칼로리로 천천히 올릴 준비를 해보세요.";
    } else if (overTargetDays >= 3) {
      focusMessage = "초과한 날이 많았어요. 주말·야식처럼 반복되는 한 패턴만 줄여도 다음 주가 편해집니다.";
    } else {
      focusMessage = "기록은 잘 이어지고 있어요. 초과한 날 다음 끼니를 가볍게 맞추는 식으로 리듬을 정리해보세요.";
    }

    return {
      loggedDays,
      adherentDays,
      overTargetDays,
      averageLoggedCalories,
      focusMessage,
    };
  },

  todayMissions(hasFoodLog: boolean, hasCoachCheckIn: boolean, weeklyReport: WeeklyAdherenceReport): TodayMission[] {
    return [
      {
        type: 'FoodLog',
        title: "오늘 음식 1개 기록",
        description: hasFoodLog ? "오늘 섭취 흐름이 시작됐어요." : "첫 식사만 적어도 남은 칼로리가 바로 보입니다.",
        actionLabel: hasFoodLog ? "기록 완료" : "음식 기록하기",
        isComplete: hasFoodLog,
      },
      {
        type: 'CoachCheckIn',
        title: "3분 코칭 체크인",
        description: hasCoachCheckIn ? "오늘 회복·근력 신호가 반영됐어요." : "체중·단백질·저항운동 중 아는 것만 입력하세요.",
        actionLabel: hasCoachCheckIn ? "체크인 완료" : "체크인 열기",
        isComplete: hasCoachCheckIn,
      },
      {
        type: 'WeeklyReview',
        title: "이번 주 리듬 확인",
        description: weeklyReport.loggedDays >= 3 ? "최근 7일 기록 흐름을 읽을 만큼 데이터가 모였어요." : "주 3회 이상 기록하면 복기 품질이 확 올라갑니다.",
        actionLabel: "주간 복기 보기",
        isComplete: weeklyReport.loggedDays >= 3,
      }
    ];
  },

  weeklyCoachingSnapshot(
    weeklyReport: WeeklyAdherenceReport,
    recoveryRisk: RecoveryRiskAssessment,
    strengthTrend: StrengthTrend,
    dietBreakRecommendation: DietBreakRecommendation
  ): WeeklyCoachingSnapshot {
    let nextAction = "";
    if (dietBreakRecommendation.shouldSuggest) {
      nextAction = `이번 주는 감량 강도보다 ${dietBreakRecommendation.suggestedDays}일 유지 전환을 먼저 검토하세요.`;
    } else if (recoveryRisk.status === 'High') {
      nextAction = "수면·피로·허기 신호가 높습니다. 오늘 체크인 후 목표 완화를 검토하세요.";
    } else if (weeklyReport.loggedDays < 3) {
      nextAction = "다음 목표는 완벽한 식단이 아니라 주 3회 기록 리듬 만들기입니다.";
    } else if (weeklyReport.overTargetDays >= 3) {
      nextAction = "초과가 반복됐습니다. 가장 자주 초과한 시간대나 상황을 한 가지 줄여보세요.";
    } else if (weeklyReport.adherentDays >= 5) {
      nextAction = "목표 이내 흐름이 좋습니다. 지금은 크게 바꾸지 말고 루틴을 유지하세요.";
    } else if (strengthTrend.status === 'Down') {
      nextAction = "근력 하락 신호가 있습니다. 훈련 볼륨과 회복을 먼저 점검하세요.";
    } else {
      nextAction = "오늘은 음식 1개 기록 and 3분 체크인만 마무리해도 충분합니다.";
    }

    let momentumLabel = "";
    if (weeklyReport.loggedDays >= 6) {
      momentumLabel = "리듬 강함";
    } else if (weeklyReport.loggedDays >= 3) {
      momentumLabel = "리듬 형성 중";
    } else if (weeklyReport.loggedDays > 0) {
      momentumLabel = "시작됨";
    } else {
      momentumLabel = "대기";
    }

    let momentumMessage = "";
    if (weeklyReport.loggedDays >= 6) {
      momentumMessage = "거의 매일 앱을 활용하고 있어요. 복기 품질이 가장 좋은 구간입니다.";
    } else if (weeklyReport.loggedDays >= 3) {
      momentumMessage = "이번 주 기록 리듬이 생겼어요. 체크인까지 더하면 코칭 정확도가 올라갑니다.";
    } else if (weeklyReport.loggedDays > 0) {
      momentumMessage = "첫 기록이 들어왔어요. 이번 주 3회 기록을 목표로 잡아보세요.";
    } else {
      momentumMessage = "아직 이번 주 기록이 없어요. 한 끼만 적어도 리포트가 살아납니다.";
    }

    return {
      title: "이번 주 코칭 스냅샷",
      summary: `기록 ${weeklyReport.loggedDays}/7일 · 목표 이내 ${weeklyReport.adherentDays}일 · 초과 ${weeklyReport.overTargetDays}일`,
      nextAction,
      momentumLabel,
      momentumMessage,
    };
  },

  calendarRhythmStatus(totalCalories: number, targetCalories: number): CalendarRhythmStatus {
    if (totalCalories <= 0) return 'Empty';
    if (totalCalories > targetCalories) return 'OverTarget';
    return 'WithinTarget';
  },

  calendarRhythmSummary(
    summaries: DailyCalorieSummary[],
    checks: DailyConditionCheck[],
    targetCalories: number
  ): CalendarRhythmSummary {
    const loggedDays = summaries.filter(s => s.totalCalories > 0).length;
    const withinTargetDays = summaries.filter(s => s.totalCalories > 0 && s.totalCalories <= targetCalories).length;
    const overTargetDays = summaries.filter(s => s.totalCalories > targetCalories).length;
    const checkInDays = checks.length;

    let message = "";
    if (loggedDays === 0 && checkInDays === 0) {
      message = "아직 월간 리듬이 비어 있어요. 오늘 한 끼와 체크인 하나만 남겨보세요.";
    } else if (loggedDays >= 20) {
      message = "이번 달 기록 리듬이 매우 안정적이에요. 복기에서 패턴을 읽기 좋습니다.";
    } else if (loggedDays >= 10) {
      message = "기록한 날이 충분히 쌓이고 있어요. 초과일과 체크인일을 함께 비교해보세요.";
    } else if (loggedDays > 0) {
      message = "기록이 시작됐어요. 빈 날도 실패가 아니라 리듬을 조정할 단서입니다.";
    } else {
      message = "체크인은 시작됐어요. 음식 기록까지 더하면 칼로리 리듬이 보입니다.";
    }

    return {
      loggedDays,
      withinTargetDays,
      overTargetDays,
      checkInDays,
      message,
    };
  },

  stateAwareReminderMessage(
    isEvening: boolean,
    currentDate: string,
    recentSummaries: DailyCalorieSummary[],
    recentChecks: DailyConditionCheck[]
  ): string {
    const todaySummary = recentSummaries.find(s => s.date === currentDate);
    const yesterdayDate = addDays(currentDate, -1);
    const yesterdaySummary = recentSummaries.find(s => s.date === yesterdayDate);
    const recoveryRisk = this.recoveryRiskAssessment(recentChecks);

    if (recoveryRisk.status === 'High') {
      return "최근 회복 신호가 높아요. 오늘은 더 줄이기보다 3분 체크인으로 수면·피로를 먼저 확인하세요.";
    }
    if (isEvening && (todaySummary?.totalCalories || 0) === 0) {
      return "오늘 기록이 아직 없어요. 한 끼만 적어도 내일 복기 흐름이 살아납니다.";
    }
    if (!isEvening && (yesterdaySummary?.totalCalories || 0) === 0) {
      return "어제 기록이 비었어요. 오늘은 첫 식사 하나만 가볍게 남기며 리듬을 다시 잡아보세요.";
    }
    if (recentSummaries.filter(s => s.totalCalories > 0).length >= 5) {
      return "이번 주 기록 리듬이 좋아요. 오늘도 짧게 확인하고 플랜 완주에 한 걸음 더 가까워지세요.";
    }
    if (isEvening) {
      return "짧은 미니컷일수록 마무리가 중요해요. 오늘 섭취와 체크인을 정리해보세요.";
    }
    return "오늘도 짧고 선명하게. 첫 식사부터 가볍게 기록해보세요.";
  },

  targetGuidance(targetCalories: number, durationWeeks: number): TargetGuidance {
    if (targetCalories <= 1100 || (durationWeeks >= 5 && targetCalories <= 1200)) {
      return {
        title: "낮은 목표예요",
        body: "긴 플랜에서 너무 낮은 목표는 피로감과 폭식을 부르기 쉬워요. 5~6주라면 한 단계 높게 잡는 편이 안정적입니다.",
        footnote: "컨디션 저하, 수면 흔들림, 운동 회복 저하가 느껴지면 바로 상향하세요.",
        tone: 'Caution',
      };
    }
    if (targetCalories >= 1500) {
      return {
        title: "상단 범위 목표예요",
        body: "활동량이 높거나 종료 직후 유지 모드로 넘어갈 때 무리 없는 선택이 될 수 있어요.",
        footnote: "감량 체감이 약하면 기록 정확도와 간식 빈도를 먼저 점검하세요.",
        tone: 'Flexible',
      };
    }
    return {
      title: "권장 범위에 들어왔어요",
      body: "2~6주 미니컷에 가장 무난한 구간입니다. 기록과 복기가 이어지기 쉬운 목표예요.",
      footnote: "배고픔이 심하지 않고 기록을 꾸준히 남길 수 있는 수준이면 가장 좋습니다.",
      tone: 'Recommended',
    };
  },

  reverseDietPlan(dailyTargetKcal: number, goalMode: MiniCutGoalModeType): ReverseDietPlan {
    const weeklyStep = goalMode === 'MassReset' ? 120 : 150;
    const summary = goalMode === 'MassReset'
      ? "리셋 목적이라면 2~3주에 걸쳐 섭취량을 천천히 올리며 다음 벌크업 준비를 시작하세요."
      : "단기 외형 목적이었더라도 종료 직후 급반등을 막으려면 단계적으로 유지 칼로리로 복귀해야 합니다.";
    const caution = "체중이 3~4일 연속 급상승하거나 폭식 충동이 커지면 증가 폭을 잠시 줄이고 수면/스트레스부터 안정화하세요.";

    const steps: ReverseDietStep[] = [1, 2, 3].map(week => {
      const target = dailyTargetKcal + weeklyStep * week;
      return {
        weekLabel: `${week}주차`,
        targetCalories: target,
        note: week === 1
          ? "식사 리듬을 먼저 안정화하고 저항운동 빈도를 유지하세요."
          : "체중·컨디션 추세를 보며 유지 칼로리에 가깝게 올리세요.",
      };
    });

    return {
      title: "종료 후 리버스 다이어트",
      summary,
      caution,
      steps,
    };
  },

  recommendedProteinGrams(weightKg: number | null): number | null {
    if (weightKg === null || weightKg <= 0) return null;
    return Math.round(weightKg * 2);
  },

  estimateMaintenanceCalories(bodyWeightKg: number | null, activityLevel: ActivityLevelType): number | null {
    if (bodyWeightKg === null || bodyWeightKg <= 0) return null;
    const factor = ActivityLevels[activityLevel].kcalPerKgFactor;
    return Math.max(1400, Math.min(4200, Math.round(bodyWeightKg * factor)));
  },

  deficitGuardrail(targetKcal: number, maintenanceKcal: number | null): DeficitGuardrail {
    if (maintenanceKcal === null || maintenanceKcal <= 0) {
      return {
        maintenanceKcal: null,
        deficitKcal: null,
        deficitPercent: null,
        level: 'Unknown',
        title: "유지 칼로리 정보가 더 필요해요",
        message: "체중과 활동 수준을 입력하면 결핍 강도를 안전 범위로 안내해드릴게요.",
        canSave: true,
      };
    }

    const deficit = Math.max(0, maintenanceKcal - targetKcal);
    const deficitPercent = (deficit * 100) / maintenanceKcal;
    const roundedPercent = Math.round(deficitPercent * 10) / 10;

    if (deficitPercent >= this.HIGH_DEFICIT_PERCENT || deficit >= this.HIGH_DEFICIT_KCAL) {
      return {
        maintenanceKcal,
        deficitKcal: deficit,
        deficitPercent: roundedPercent,
        level: 'High',
        title: "결핍 강도가 과도해요",
        message: `현재 설정은 유지 대비 ${deficit}kcal(${roundedPercent}%) 결핍으로 추정됩니다. 근손실·피로 리스크를 줄이려면 목표를 높여주세요.`,
        canSave: false,
      };
    }

    if (deficitPercent >= this.CAUTION_DEFICIT_PERCENT || deficit >= this.CAUTION_DEFICIT_KCAL) {
      return {
        maintenanceKcal,
        deficitKcal: deficit,
        deficitPercent: roundedPercent,
        level: 'Caution',
        title: "결핍 강도 주의 구간",
        message: `유지 대비 ${deficit}kcal(${roundedPercent}%) 결핍입니다. 수면·훈련 회복 신호를 더 자주 확인하세요.`,
        canSave: true,
      };
    }

    return {
      maintenanceKcal,
      deficitKcal: deficit,
      deficitPercent: roundedPercent,
      level: 'Safe',
      title: "결핍 강도 안전 구간",
      message: `유지 대비 ${deficit}kcal(${roundedPercent}%) 결핍으로 안정적인 범위에 가깝습니다.`,
      canSave: true,
    };
  },

  recoveryRiskAssessment(checks: DailyConditionCheck[]): RecoveryRiskAssessment {
    const recentChecks = [...checks].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 3);
    if (recentChecks.length < 2) {
      return {
        status: 'NoData',
        flaggedDays: 0,
        message: "수면·피로·허기 체크가 쌓이면 회복 리스크를 자동 분석해요.",
        suggestDietBreak: false,
      };
    }

    const dayRiskScores = recentChecks.map(check => {
      let score = 0;
      if (check.sleepHours !== null && check.sleepHours < 6) score += 1;
      if (check.fatigueScore !== null && check.fatigueScore >= 4) score += 1;
      if (check.hungerScore !== null && check.hungerScore >= 4) score += 1;
      if (check.moodScore !== null && check.moodScore <= 2) score += 1;
      if (check.workoutPerformanceScore !== null && check.workoutPerformanceScore <= 2) score += 1;
      return score;
    });

    const flaggedDays = dayRiskScores.filter(s => s > 0).length;
    const highDays = dayRiskScores.filter(s => s >= 2).length;

    if (highDays >= 2) {
      return {
        status: 'High',
        flaggedDays,
        message: "최근 3일 회복 레드플래그가 반복돼요. 목표 칼로리 완화 또는 3~7일 다이어트 브레이크를 권장합니다.",
        suggestDietBreak: true,
      };
    }

    if (flaggedDays >= 2) {
      return {
        status: 'Watch',
        flaggedDays,
        message: "회복 신호가 누적되고 있어요. 수면·훈련 강도를 점검하고 감량 강도를 미세 완화해보세요.",
        suggestDietBreak: false,
      };
    }

    return {
      status: 'Stable',
      flaggedDays,
      message: "최근 회복 신호는 안정적이에요. 현재 루틴을 유지하세요.",
      suggestDietBreak: false,
    };
  },

  weeklyWeightTrend(checks: DailyConditionCheck[]): WeeklyWeightTrend {
    const weighted = checks.filter(c => c.bodyWeightKg !== null && c.bodyWeightKg > 0).sort((a, b) => a.date.localeCompare(b.date));
    if (weighted.length < 2) {
      return {
        status: 'NoData',
        ratePercentPerWeek: null,
        message: "체중 기록이 2회 이상 쌓이면 주간 감량 속도를 계산할 수 있어요.",
      };
    }

    const first = weighted[0];
    const last = weighted[weighted.length - 1];
    const startWeight = first.bodyWeightKg!;
    const endWeight = last.bodyWeightKg!;
    const daySpan = daysBetween(first.date, last.date);

    if (daySpan < 3) {
      return {
        status: 'NoData',
        ratePercentPerWeek: null,
        message: "최소 3일 이상 간격의 체중 2회 기록이 있어야 속도 판단이 정확해져요.",
      };
    }

    const lossPercentOverPeriod = ((startWeight - endWeight) / startWeight) * 100;
    const normalizedWeeklyLoss = lossPercentOverPeriod * (7 / daySpan);
    const rounded = Math.round(normalizedWeeklyLoss * 100) / 100;

    if (normalizedWeeklyLoss <= 0) {
      return {
        status: 'GainOrStall',
        ratePercentPerWeek: rounded,
        message: "최근 체중이 정체/증가 추세예요. 간식·야식 패턴과 기록 정확도를 먼저 점검하세요.",
      };
    }

    if (normalizedWeeklyLoss < 0.75) {
      return {
        status: 'TooSlow',
        ratePercentPerWeek: rounded,
        message: "주간 감량 속도가 느린 편이에요. 활동량·기록 누락을 점검하고 다음 주에 미세 조정해보세요.",
      };
    }

    if (normalizedWeeklyLoss <= 1.25) {
      return {
        status: 'InRange',
        ratePercentPerWeek: rounded,
        message: "권장 감량 속도(주당 0.75~1.25%) 범위에 있어요. 현재 리듬을 유지하세요.",
      };
    }

    return {
      status: 'TooFast',
      ratePercentPerWeek: rounded,
      message: "감량 속도가 너무 빨라요. 근손실/반동 위험을 줄이기 위해 섭취량을 소폭 올리는 것을 권장합니다.",
    };
  },

  strengthTrend(checks: DailyConditionCheck[]): StrengthTrend {
    const records = checks.filter(c => c.mainLiftKg !== null && c.mainLiftKg > 0).sort((a, b) => a.date.localeCompare(b.date));
    if (records.length < 2) {
      return {
        status: 'NoData',
        changePercent: null,
        message: "핵심 리프트 기록이 쌓이면 주간 근력 추세를 분석해요.",
      };
    }

    const first = records[0].mainLiftKg!;
    const last = records[records.length - 1].mainLiftKg!;

    const changePercent = ((last - first) / first) * 100;
    const rounded = Math.round(changePercent * 10) / 10;

    if (changePercent >= 2.0) {
      return {
        status: 'Up',
        changePercent: rounded,
        message: `핵심 리프트가 상승 추세예요 (+${rounded}%). 감량 중 근력 방어가 잘 되고 있습니다.`,
      };
    }

    if (changePercent <= -2.0) {
      return {
        status: 'Down',
        changePercent: rounded,
        message: `핵심 리프트가 하락 추세예요 (${rounded}%). 회복/브레이크/목표 완화를 우선 점검하세요.`,
      };
    }

    return {
      status: 'Stable',
      changePercent: rounded,
      message: `핵심 리프트가 안정적이에요 (${rounded}%). 현재 감량 리듬을 유지하세요.`,
    };
  },

  relapsePreventionInsight(checks: DailyConditionCheck[]): RelapsePreventionInsight {
    const triggers: Record<string, number> = {};
    checks.forEach(c => {
      if (c.relapseTrigger && c.relapseTrigger.trim().length > 0) {
        const key = c.relapseTrigger.trim();
        triggers[key] = (triggers[key] || 0) + 1;
      }
    });

    const triggerPairs = Object.entries(triggers);
    if (triggerPairs.length === 0) {
      return {
        recurringTrigger: null,
        recommendedAction: null,
        triggerCount: 0,
        message: "트리거를 기록하면 패턴과 대응 루틴을 제안해요.",
      };
    }

    // Sort by count descending
    triggerPairs.sort((a, b) => b[1] - a[1]);
    const [topTrigger, count] = triggerPairs[0];
    const action = RelapsePreventionCatalog.recommendedActionFor(topTrigger);

    const message = count >= 2
      ? `최근 반복 트리거는 '${topTrigger}' (${count}회)예요. 대응 루틴을 먼저 고정하면 폭식/이탈을 줄이기 쉽습니다.`
      : `가장 최근 기록된 트리거는 '${topTrigger}'예요. 같은 상황이 오기 전에 대응 루틴을 준비해보세요.`;

    return {
      recurringTrigger: topTrigger,
      recommendedAction: action,
      triggerCount: count,
      message,
    };
  },

  calorieAdjustmentRecommendation(
    currentTargetKcal: number,
    weeklyWeightTrend: WeeklyWeightTrend
  ): CalorieAdjustmentRecommendation {
    const clampedCurrent = Math.max(this.RECOMMENDED_MIN_KCAL, Math.min(this.RECOMMENDED_MAX_KCAL, currentTargetKcal));
    const targetOptions = [...this.TARGET_OPTIONS_KCAL].sort((a, b) => a - b);
    const nextLowerOption = [...targetOptions].reverse().find(o => o < clampedCurrent);
    const nextHigherOption = targetOptions.find(o => o > clampedCurrent);

    switch (weeklyWeightTrend.status) {
      case 'NoData':
        return {
          currentTargetKcal: clampedCurrent,
          suggestedTargetKcal: clampedCurrent,
          direction: 'Keep',
          deltaKcal: 0,
          title: "데이터 수집 중",
          message: "체중 체크인이 더 쌓이면 목표 칼로리 미세 조정 제안을 자동으로 제공합니다.",
          actionable: false,
        };

      case 'InRange':
        return {
          currentTargetKcal: clampedCurrent,
          suggestedTargetKcal: clampedCurrent,
          direction: 'Keep',
          deltaKcal: 0,
          title: "현재 목표 유지",
          message: "감량 속도가 권장 범위입니다. 이번 주는 목표 칼로리를 유지하세요.",
          actionable: false,
        };

      case 'TooSlow':
      case 'GainOrStall': {
        const suggested = nextLowerOption !== undefined ? nextLowerOption : clampedCurrent;
        const delta = clampedCurrent - suggested;
        return {
          currentTargetKcal: clampedCurrent,
          suggestedTargetKcal: suggested,
          direction: delta > 0 ? 'Decrease' : 'Keep',
          deltaKcal: delta,
          title: delta > 0 ? "목표 칼로리 소폭 하향 제안" : "최저 구간 도달",
          message: delta > 0
            ? `다음 7일은 하루 목표를 ${delta}kcal 낮춰 반응을 확인해보세요.`
            : "이미 권장 하한(1000kcal)에 있어요. 칼로리 추가 하향보다 기록 정확도·활동량을 먼저 점검하세요.",
          actionable: delta > 0,
        };
      }

      case 'TooFast': {
        const suggested = nextHigherOption !== undefined ? nextHigherOption : clampedCurrent;
        const delta = suggested - clampedCurrent;
        return {
          currentTargetKcal: clampedCurrent,
          suggestedTargetKcal: suggested,
          direction: delta > 0 ? 'Increase' : 'Keep',
          deltaKcal: delta,
          title: delta > 0 ? "목표 칼로리 소폭 상향 제안" : "상한 구간 도달",
          message: delta > 0
            ? `근손실/피로 위험을 줄이기 위해 다음 7일은 하루 목표를 ${delta}kcal 올려보세요.`
            : "이미 상단 목표(1500kcal)입니다. 피로가 크면 휴식·수면·훈련 볼륨부터 조정하세요.",
          actionable: delta > 0,
        };
      }
    }
  },

  recoveryAwareCalorieAdjustmentRecommendation(
    currentTargetKcal: number,
    weeklyWeightTrend: WeeklyWeightTrend,
    recoveryRisk: RecoveryRiskAssessment
  ): CalorieAdjustmentRecommendation {
    const baseline = this.calorieAdjustmentRecommendation(currentTargetKcal, weeklyWeightTrend);
    if (recoveryRisk.status !== 'High') {
      return baseline;
    }

    const targetOptions = [...this.TARGET_OPTIONS_KCAL].sort((a, b) => a - b);
    const higherOption = targetOptions.find(o => o > baseline.currentTargetKcal) || baseline.currentTargetKcal;
    const delta = Math.max(0, higherOption - baseline.currentTargetKcal);

    if (delta > 0) {
      return {
        ...baseline,
        suggestedTargetKcal: higherOption,
        direction: 'Increase',
        deltaKcal: delta,
        title: "회복 레드플래그: 목표 완화 권고",
        message: `최근 회복 신호가 누적되어 ${delta}kcal 상향을 우선 권장합니다. 필요하면 3~7일 다이어트 브레이크를 진행하세요.`,
        actionable: true,
      };
    }

    return {
      ...baseline,
      direction: 'Keep',
      title: "회복 우선 구간",
      message: "이미 상단 목표 구간입니다. 이번 주는 추가 하향 없이 회복(수면/피로 관리)을 우선하세요.",
      actionable: false,
    };
  },

  leanMassProtectionScore(
    checks: DailyConditionCheck[],
    recommendedProteinGrams: number | null,
    recoveryRisk: RecoveryRiskAssessment
  ): LeanMassProtectionScore {
    const recent = [...checks].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 7);
    if (recent.length < 2) {
      return {
        score: 0,
        grade: 'NoData',
        message: "단백질/훈련 체크가 쌓이면 근손실 방어 점수를 계산해요.",
        proteinHitDays: 0,
        resistanceHitDays: 0,
      };
    }

    const proteinHitDays = recent.filter(check => {
      const protein = check.proteinGrams || 0;
      const rec = recommendedProteinGrams || 0;
      return rec > 0 && protein >= rec;
    }).length;

    const resistanceHitDays = recent.filter(c => (c.resistanceSets || 0) >= 8).length;
    const checkinDays = recent.filter(c => (c.proteinGrams || 0) > 0 || (c.resistanceSets || 0) > 0 || (c.bodyWeightKg || 0) > 0).length;

    const proteinScore = Math.min(50, proteinHitDays * 10);
    const resistanceScore = Math.min(30, resistanceHitDays * 10);
    const consistencyScore = Math.min(20, checkinDays * 3);

    let penalty = 0;
    if (recoveryRisk.status === 'High') penalty = 20;
    else if (recoveryRisk.status === 'Watch') penalty = 10;

    const score = Math.max(0, Math.min(100, proteinScore + resistanceScore + consistencyScore - penalty));

    let grade: LeanMassProtectionGrade = 'Low';
    if (score >= 80) grade = 'Excellent';
    else if (score >= 65) grade = 'Good';
    else if (score >= 45) grade = 'Moderate';

    let message = "";
    switch (grade) {
      case 'Excellent':
        message = "근손실 방어 루틴이 매우 안정적이에요. 현재 패턴을 유지하세요.";
        break;
      case 'Good':
        message = "근손실 방어가 잘 되고 있어요. 단백질/훈련 한두 날만 더 보강하면 더 좋아집니다.";
        break;
      case 'Moderate':
        message = "기본 루틴은 유지 중이지만 단백질 또는 훈련 달성률 보강이 필요해요.";
        break;
      case 'Low':
        message = "근손실 방어 지표가 약해요. 단백질·저항운동·회복 신호를 우선 개선하세요.";
        break;
    }

    return {
      score,
      grade,
      message,
      proteinHitDays,
      resistanceHitDays,
    };
  },

  dietBreakRecommendation(
    phase: MiniCutPhase | null,
    recoveryRisk: RecoveryRiskAssessment,
    weeklyWeightTrend: WeeklyWeightTrend
  ): DietBreakRecommendation {
    if (phase !== 'Active') {
      return {
        shouldSuggest: false,
        suggestedDays: 0,
        title: "브레이크 불필요",
        message: "계획이 활성화되어 있지 않습니다.",
      };
    }

    if (recoveryRisk.status === 'High') {
      return {
        shouldSuggest: true,
        suggestedDays: 5,
        title: "5일 다이어트 브레이크 권장",
        message: "회복 레드플래그가 누적되었습니다. 3~7일 유지칼로리 구간으로 전환 후 감량을 재개하세요.",
      };
    }

    if (weeklyWeightTrend.status === 'TooFast' && recoveryRisk.status === 'Watch') {
      return {
        shouldSuggest: true,
        suggestedDays: 3,
        title: "3일 미니 브레이크 권장",
        message: "감량 속도 과속 + 회복 경고가 함께 보여 짧은 유지구간으로 피로를 완화하는 것이 좋습니다.",
      };
    }

    return {
      shouldSuggest: false,
      suggestedDays: 0,
      title: "브레이크 불필요",
      message: "현재는 감량 리듬이 안정적입니다. 체크인을 유지하며 진행하세요.",
    };
  }
};
