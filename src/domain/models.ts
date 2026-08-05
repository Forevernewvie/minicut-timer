export type ActivityLevelType = 'Low' | 'Moderate' | 'High';

export interface ActivityLevelConfig {
  displayName: string;
  kcalPerKgFactor: number;
}

export const ActivityLevels: Record<ActivityLevelType, ActivityLevelConfig> = {
  Low: {
    displayName: "낮음(좌식 위주)",
    kcalPerKgFactor: 28,
  },
  Moderate: {
    displayName: "보통(주 3~4회 활동)",
    kcalPerKgFactor: 31,
  },
  High: {
    displayName: "높음(주 5회+ 훈련)",
    kcalPerKgFactor: 34,
  },
};

export type MiniCutGoalModeType = 'MassReset' | 'EventReady';

export interface MiniCutGoalModeConfig {
  displayName: string;
  shortDescription: string;
}

export const MiniCutGoalModes: Record<MiniCutGoalModeType, MiniCutGoalModeConfig> = {
  MassReset: {
    displayName: "매스업 리셋",
    shortDescription: "다음 벌크업 효율 회복",
  },
  EventReady: {
    displayName: "단기 외형 개선",
    shortDescription: "촬영·휴가 등 이벤트 대비",
  },
};

export interface MiniCutPlan {
  startDate: string; // YYYY-MM-DD
  durationWeeks: number;
  endDate: string; // YYYY-MM-DD
  dailyTargetKcal: number;
  goalMode: MiniCutGoalModeType;
  activityLevel: ActivityLevelType;
  estimatedMaintenanceKcal: number;
  isActive: boolean;
}

export interface CalorieEntry {
  id: number;
  date: string; // YYYY-MM-DD
  calories: number;
  foodName: string;
  note: string;
  timeLabel: string;
  isFavorite: boolean;
  createdAt: string; // ISO String
}

export interface DailyCalorieSummary {
  date: string; // YYYY-MM-DD
  totalCalories: number;
  entryCount: number;
}

export type CalorieRangeStatus = 'NoData' | 'Below' | 'InRange' | 'Above';

export type MiniCutPhase = 'Upcoming' | 'Active' | 'Completed';

export interface EntryQuickPreset {
  foodName: string;
  calories: number;
  note: string;
  timeLabel: string;
  isFavorite: boolean;
}

export interface WeeklyAdherenceReport {
  loggedDays: number;
  adherentDays: number;
  overTargetDays: number;
  averageLoggedCalories: number;
  focusMessage: string;
}

export type TargetGuidanceTone = 'Caution' | 'Recommended' | 'Flexible';

export interface TargetGuidance {
  title: string;
  body: string;
  footnote: string;
  tone: TargetGuidanceTone;
}

export interface ReverseDietStep {
  weekLabel: string;
  targetCalories: number;
  note: string;
}

export interface ReverseDietPlan {
  title: string;
  summary: string;
  caution: string;
  steps: ReverseDietStep[];
}

export interface DailyConditionCheck {
  date: string; // YYYY-MM-DD
  bodyWeightKg: number | null;
  proteinGrams: number | null;
  resistanceSets: number | null;
  mainLiftKg: number | null;
  relapseTrigger: string | null;
  copingAction: string | null;
  sleepHours: number | null;
  fatigueScore: number | null;
  hungerScore: number | null;
  moodScore: number | null;
  workoutPerformanceScore: number | null;
  updatedAt: string; // ISO String
}

export type StrengthTrendStatus = 'NoData' | 'Up' | 'Stable' | 'Down';

export interface StrengthTrend {
  status: StrengthTrendStatus;
  changePercent: number | null;
  message: string;
}

export interface RelapsePreventionInsight {
  recurringTrigger: string | null;
  recommendedAction: string | null;
  triggerCount: number;
  message: string;
}

export type WeeklyWeightTrendStatus = 'NoData' | 'TooSlow' | 'InRange' | 'TooFast' | 'GainOrStall';

export interface WeeklyWeightTrend {
  status: WeeklyWeightTrendStatus;
  ratePercentPerWeek: number | null;
  message: string;
}

export type CalorieAdjustmentDirection = 'Keep' | 'Increase' | 'Decrease';

export interface CalorieAdjustmentRecommendation {
  currentTargetKcal: number;
  suggestedTargetKcal: number;
  direction: CalorieAdjustmentDirection;
  deltaKcal: number;
  title: string;
  message: string;
  actionable: boolean;
}

export type DeficitRiskLevel = 'Unknown' | 'Safe' | 'Caution' | 'High';

export interface DeficitGuardrail {
  maintenanceKcal: number | null;
  deficitKcal: number | null;
  deficitPercent: number | null;
  level: DeficitRiskLevel;
  title: string;
  message: string;
  canSave: boolean;
}

export type RecoveryRiskStatus = 'NoData' | 'Stable' | 'Watch' | 'High';

export interface RecoveryRiskAssessment {
  status: RecoveryRiskStatus;
  flaggedDays: number;
  message: string;
  suggestDietBreak: boolean;
}

export type LeanMassProtectionGrade = 'NoData' | 'Low' | 'Moderate' | 'Good' | 'Excellent';

export interface LeanMassProtectionScore {
  score: number;
  grade: LeanMassProtectionGrade;
  message: string;
  proteinHitDays: number;
  resistanceHitDays: number;
}

export interface DietBreakRecommendation {
  shouldSuggest: boolean;
  suggestedDays: number;
  title: string;
  message: string;
}

export type MissionType = 'FoodLog' | 'CoachCheckIn' | 'WeeklyReview';

export interface TodayMission {
  type: MissionType;
  title: string;
  description: string;
  actionLabel: string;
  isComplete: boolean;
}

export interface PlanProgressSnapshot {
  phase: MiniCutPhase;
  progress: number;
  elapsedDays: number;
  remainingDays: number;
  dDayLabel: string;
  headline: string;
  supportingText: string;
}

export interface WeeklyCoachingSnapshot {
  title: string;
  summary: string;
  nextAction: string;
  momentumLabel: string;
  momentumMessage: string;
}

export type CalendarRhythmStatus = 'Empty' | 'WithinTarget' | 'OverTarget';

export interface CalendarRhythmSummary {
  loggedDays: number;
  withinTargetDays: number;
  overTargetDays: number;
  checkInDays: number;
  message: string;
}
