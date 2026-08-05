import { create } from 'zustand';
import { MiniCutRepository } from '../repository/MiniCutRepository';
import {
  MiniCutPlan,
  CalorieEntry,
  DailyCalorieSummary,
  DailyConditionCheck,
  EntryQuickPreset,
  TodayMission,
  WeeklyAdherenceReport,
  WeeklyCoachingSnapshot,
  MiniCutGoalModeType,
  ActivityLevelType,
  CalendarRhythmSummary
} from '../domain/models';
import { MiniCutRules, toLocalDateString, addDays, daysBetween } from '../domain/rules';

interface MiniCutState {
  plan: MiniCutPlan | null;
  selectedDate: string; // YYYY-MM-DD
  selectedEntries: CalorieEntry[];
  selectedTotalCalories: number;
  selectedConditionCheck: DailyConditionCheck | null;
  
  recentPresets: EntryQuickPreset[];
  favoritePresets: EntryQuickPreset[];
  
  weeklyReport: WeeklyAdherenceReport | null;
  weeklyCoaching: WeeklyCoachingSnapshot | null;
  todayMissions: TodayMission[];
  
  calendarSummaries: DailyCalorieSummary[];
  calendarChecks: DailyConditionCheck[];
  calendarRhythm: CalendarRhythmSummary | null;

  isLoading: boolean;
  error: string | null;

  // Actions
  loadInitialData: () => Promise<void>;
  setSelectedDate: (date: string) => Promise<void>;
  loadSelectedDateData: (date: string) => Promise<void>;
  
  savePlan: (
    startDate: string,
    durationWeeks: number,
    dailyTargetKcal: number,
    goalMode: MiniCutGoalModeType,
    activityLevel: ActivityLevelType,
    estimatedMaintenanceKcal: number
  ) => Promise<void>;
  
  addEntry: (
    calories: number,
    foodName: string,
    note: string,
    timeLabel: string
  ) => Promise<void>;
  
  addEntryFromPreset: (preset: EntryQuickPreset) => Promise<void>;
  
  updateEntry: (
    entry: CalorieEntry,
    calories: number,
    foodName: string,
    note: string,
    timeLabel: string
  ) => Promise<void>;
  
  toggleEntryFavorite: (entryId: number, isFavorite: boolean) => Promise<void>;
  
  deleteEntry: (entryId: number) => Promise<void>;
  
  upsertConditionCheck: (check: Partial<DailyConditionCheck>) => Promise<void>;
  
  clearAllData: () => Promise<void>;
}

export const useMiniCutStore = create<MiniCutState>((set, get) => ({
  plan: null,
  selectedDate: toLocalDateString(new Date()),
  selectedEntries: [],
  selectedTotalCalories: 0,
  selectedConditionCheck: null,
  
  recentPresets: [],
  favoritePresets: [],
  
  weeklyReport: null,
  weeklyCoaching: null,
  todayMissions: [],
  
  calendarSummaries: [],
  calendarChecks: [],
  calendarRhythm: null,

  isLoading: false,
  error: null,

  loadInitialData: async () => {
    set({ isLoading: true, error: null });
    try {
      const plan = await MiniCutRepository.getPlan();
      const todayStr = toLocalDateString(new Date());
      const selectedDate = get().selectedDate || todayStr;
      
      const recentPresets = await MiniCutRepository.getRecentEntryPresets(4);
      const favoritePresets = await MiniCutRepository.getFavoriteEntryPresets(4);
      
      set({ plan, recentPresets, favoritePresets });
      
      await get().loadSelectedDateData(selectedDate);
      await get().setSelectedDate(selectedDate);
    } catch (err: any) {
      set({ error: err.message || 'Failed to load initial data' });
    } finally {
      set({ isLoading: false });
    }
  },

  setSelectedDate: async (date: string) => {
    set({ selectedDate: date });
    await get().loadSelectedDateData(date);
  },

  loadSelectedDateData: async (date: string) => {
    try {
      const entries = await MiniCutRepository.getEntriesForDate(date);
      const total = await MiniCutRepository.getTodayTotal(date);
      const check = await MiniCutRepository.getDailyConditionCheck(date);
      
      set({
        selectedEntries: entries,
        selectedTotalCalories: total,
        selectedConditionCheck: check
      });

      // Recalculate weekly and calendar reports
      const plan = get().plan;
      const todayStr = toLocalDateString(new Date());
      
      // Calculate 7-day window for weekly coaching (today - 6 days to today)
      const weeklyStartDate = addDays(todayStr, -6);
      const weeklySummaries = await MiniCutRepository.getDailySummaries(weeklyStartDate, todayStr);
      const weeklyChecks = await MiniCutRepository.getDailyConditionChecks(weeklyStartDate, todayStr);
      
      const targetKcal = plan ? plan.dailyTargetKcal : MiniCutRules.DEFAULT_TARGET_KCAL;
      const weeklyReport = MiniCutRules.weeklyAdherenceReport(weeklySummaries, targetKcal);
      
      // Compute insights for coaching
      const recoveryRisk = MiniCutRules.recoveryRiskAssessment(weeklyChecks);
      const weightTrend = MiniCutRules.weeklyWeightTrend(weeklyChecks);
      const strengthTrend = MiniCutRules.strengthTrend(weeklyChecks);
      
      const planPhase = plan ? MiniCutRules.phaseOf(plan.startDate, plan.endDate, todayStr) : null;
      const dietBreak = MiniCutRules.dietBreakRecommendation(planPhase, recoveryRisk, weightTrend);
      
      const weeklyCoaching = MiniCutRules.weeklyCoachingSnapshot(
        weeklyReport,
        recoveryRisk,
        strengthTrend,
        dietBreak
      );

      // Check if food logged today
      const todayTotal = await MiniCutRepository.getTodayTotal(todayStr);
      const todayCheck = await MiniCutRepository.getDailyConditionCheck(todayStr);
      const hasFoodLog = todayTotal > 0;
      const hasCheckIn = todayCheck !== null;
      
      const todayMissions = MiniCutRules.todayMissions(hasFoodLog, hasCheckIn, weeklyReport);

      // Calendar stats: Use 30 days window or plan window if active
      let calStart = addDays(todayStr, -15);
      let calEnd = addDays(todayStr, 15);
      if (plan) {
        calStart = plan.startDate;
        calEnd = plan.endDate;
      }
      const calendarSummaries = await MiniCutRepository.getDailySummaries(calStart, calEnd);
      const calendarChecks = await MiniCutRepository.getDailyConditionChecks(calStart, calEnd);
      const calendarRhythm = MiniCutRules.calendarRhythmSummary(calendarSummaries, calendarChecks, targetKcal);

      set({
        weeklyReport,
        weeklyCoaching,
        todayMissions,
        calendarSummaries,
        calendarChecks,
        calendarRhythm
      });
    } catch (err: any) {
      set({ error: err.message || 'Failed to load date data' });
    }
  },

  savePlan: async (startDate, durationWeeks, dailyTargetKcal, goalMode, activityLevel, estimatedMaintenanceKcal) => {
    set({ isLoading: true, error: null });
    try {
      await MiniCutRepository.savePlan(
        startDate,
        durationWeeks,
        dailyTargetKcal,
        goalMode,
        activityLevel,
        estimatedMaintenanceKcal
      );
      await get().loadInitialData();
    } catch (err: any) {
      set({ error: err.message || 'Failed to save plan' });
      throw err;
    } finally {
      set({ isLoading: false });
    }
  },

  addEntry: async (calories, foodName, note, timeLabel) => {
    const date = get().selectedDate;
    try {
      await MiniCutRepository.addEntry(date, calories, foodName, note, timeLabel);
      
      // Refresh presets
      const recent = await MiniCutRepository.getRecentEntryPresets(4);
      set({ recentPresets: recent });
      
      await get().loadSelectedDateData(date);
    } catch (err: any) {
      set({ error: err.message || 'Failed to add entry' });
    }
  },

  addEntryFromPreset: async (preset) => {
    const date = get().selectedDate;
    try {
      await MiniCutRepository.addEntryFromPreset(date, preset);
      await get().loadSelectedDateData(date);
    } catch (err: any) {
      set({ error: err.message || 'Failed to add entry from preset' });
    }
  },

  updateEntry: async (entry, calories, foodName, note, timeLabel) => {
    const date = get().selectedDate;
    try {
      await MiniCutRepository.updateEntry(entry, calories, foodName, note, timeLabel);
      await get().loadSelectedDateData(date);
    } catch (err: any) {
      set({ error: err.message || 'Failed to update entry' });
    }
  },

  toggleEntryFavorite: async (entryId, isFavorite) => {
    const date = get().selectedDate;
    try {
      await MiniCutRepository.updateEntryFavorite(entryId, isFavorite);
      
      // Refresh presets
      const recent = await MiniCutRepository.getRecentEntryPresets(4);
      const favorite = await MiniCutRepository.getFavoriteEntryPresets(4);
      set({ recentPresets: recent, favoritePresets: favorite });
      
      await get().loadSelectedDateData(date);
    } catch (err: any) {
      set({ error: err.message || 'Failed to toggle favorite' });
    }
  },

  deleteEntry: async (entryId) => {
    const date = get().selectedDate;
    try {
      await MiniCutRepository.deleteEntry(entryId);
      await get().loadSelectedDateData(date);
    } catch (err: any) {
      set({ error: err.message || 'Failed to delete entry' });
    }
  },

  upsertConditionCheck: async (check) => {
    const date = get().selectedDate;
    const current = get().selectedConditionCheck;
    
    // Merge values
    const weight = check.bodyWeightKg !== undefined ? check.bodyWeightKg : (current?.bodyWeightKg ?? null);
    const protein = check.proteinGrams !== undefined ? check.proteinGrams : (current?.proteinGrams ?? null);
    const sets = check.resistanceSets !== undefined ? check.resistanceSets : (current?.resistanceSets ?? null);
    const lift = check.mainLiftKg !== undefined ? check.mainLiftKg : (current?.mainLiftKg ?? null);
    const trigger = check.relapseTrigger !== undefined ? check.relapseTrigger : (current?.relapseTrigger ?? null);
    const action = check.copingAction !== undefined ? check.copingAction : (current?.copingAction ?? null);
    const sleep = check.sleepHours !== undefined ? check.sleepHours : (current?.sleepHours ?? null);
    const fatigue = check.fatigueScore !== undefined ? check.fatigueScore : (current?.fatigueScore ?? null);
    const hunger = check.hungerScore !== undefined ? check.hungerScore : (current?.hungerScore ?? null);
    const mood = check.moodScore !== undefined ? check.moodScore : (current?.moodScore ?? null);
    const performance = check.workoutPerformanceScore !== undefined ? check.workoutPerformanceScore : (current?.workoutPerformanceScore ?? null);

    try {
      await MiniCutRepository.upsertDailyConditionCheck(
        date,
        weight,
        protein,
        sets,
        lift,
        trigger,
        action,
        sleep,
        fatigue,
        hunger,
        mood,
        performance
      );
      await get().loadSelectedDateData(date);
    } catch (err: any) {
      set({ error: err.message || 'Failed to update condition check' });
    }
  },

  clearAllData: async () => {
    set({ isLoading: true });
    try {
      await MiniCutRepository.clearAllSavedData();
      set({
        plan: null,
        selectedEntries: [],
        selectedTotalCalories: 0,
        selectedConditionCheck: null,
        recentPresets: [],
        favoritePresets: [],
        weeklyReport: null,
        weeklyCoaching: null,
        todayMissions: [],
        calendarSummaries: [],
        calendarChecks: [],
        calendarRhythm: null
      });
      await get().loadInitialData();
    } catch (err: any) {
      set({ error: err.message || 'Failed to clear data' });
    } finally {
      set({ isLoading: false });
    }
  }
}));
