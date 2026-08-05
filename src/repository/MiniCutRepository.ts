import { getDatabase } from '../database/db';
import {
  MiniCutPlan,
  CalorieEntry,
  DailyCalorieSummary,
  DailyConditionCheck,
  EntryQuickPreset,
  MiniCutGoalModeType,
  ActivityLevelType
} from '../domain/models';
import { MiniCutRules } from '../domain/rules';

export class MiniCutRepository {
  // Plan Operations
  static async getPlan(): Promise<MiniCutPlan | null> {
    const db = await getDatabase();
    const row = await db.getFirstAsync<any>(
      'SELECT * FROM mini_cut_plan WHERE isActive = 1 LIMIT 1'
    );
    if (!row) return null;
    return {
      startDate: row.startDate,
      durationWeeks: row.durationWeeks,
      endDate: row.endDate,
      dailyTargetKcal: row.dailyTargetKcal,
      goalMode: row.goalMode as MiniCutGoalModeType,
      activityLevel: row.activityLevel as ActivityLevelType,
      estimatedMaintenanceKcal: row.estimatedMaintenanceKcal,
      isActive: row.isActive === 1
    };
  }

  static async savePlan(
    startDate: string,
    durationWeeks: number,
    dailyTargetKcal: number,
    goalMode: MiniCutGoalModeType = 'MassReset',
    activityLevel: ActivityLevelType = 'Moderate',
    estimatedMaintenanceKcal: number = 0
  ): Promise<void> {
    const endDate = MiniCutRules.calculateEndDate(startDate, durationWeeks);
    if (!MiniCutRules.isValidTarget(dailyTargetKcal)) {
      throw new Error(`하루 목표 칼로리는 ${MiniCutRules.RECOMMENDED_MIN_KCAL}~${MiniCutRules.RECOMMENDED_MAX_KCAL} 범위여야 합니다.`);
    }

    const db = await getDatabase();
    // Use transaction/run to upsert the single plan (id = 1)
    await db.runAsync(
      `INSERT OR REPLACE INTO mini_cut_plan 
       (id, startDate, durationWeeks, endDate, dailyTargetKcal, goalMode, activityLevel, estimatedMaintenanceKcal, isActive) 
       VALUES (1, ?, ?, ?, ?, ?, ?, ?, 1)`,
      [startDate, durationWeeks, endDate, dailyTargetKcal, goalMode, activityLevel, estimatedMaintenanceKcal]
    );
  }

  // Calorie Entry Operations
  static async getEntriesForDate(date: string): Promise<CalorieEntry[]> {
    const db = await getDatabase();
    const rows = await db.getAllAsync<any>(
      'SELECT * FROM calorie_entries WHERE date = ? ORDER BY createdAt ASC',
      [date]
    );
    return rows.map(row => ({
      id: row.id,
      date: row.date,
      calories: row.calories,
      foodName: row.foodName,
      note: row.note,
      timeLabel: row.timeLabel,
      isFavorite: row.isFavorite === 1,
      createdAt: row.createdAt
    }));
  }

  static async getTodayTotal(todayStr: string): Promise<number> {
    const db = await getDatabase();
    const result = await db.getFirstAsync<any>(
      'SELECT SUM(calories) as total FROM calorie_entries WHERE date = ?',
      [todayStr]
    );
    return result?.total || 0;
  }

  static async getRecentEntryPresets(limit: number = 4): Promise<EntryQuickPreset[]> {
    const db = await getDatabase();
    const rows = await db.getAllAsync<any>(
      'SELECT foodName, calories, note, timeLabel, isFavorite FROM calorie_entries ORDER BY id DESC LIMIT ?',
      [limit * 3]
    );
    return this.toQuickPresets(rows, limit);
  }

  static async getFavoriteEntryPresets(limit: number = 4): Promise<EntryQuickPreset[]> {
    const db = await getDatabase();
    const rows = await db.getAllAsync<any>(
      'SELECT foodName, calories, note, timeLabel, isFavorite FROM calorie_entries WHERE isFavorite = 1 ORDER BY id DESC LIMIT ?',
      [limit * 3]
    );
    return this.toQuickPresets(rows, limit).map(p => ({ ...p, isFavorite: true }));
  }

  static async getDailySummaries(startDate: string, endDate: string): Promise<DailyCalorieSummary[]> {
    const db = await getDatabase();
    const rows = await db.getAllAsync<any>(
      `SELECT date, SUM(calories) as totalCalories, COUNT(id) as entryCount 
       FROM calorie_entries 
       WHERE date BETWEEN ? AND ? 
       GROUP BY date`,
      [startDate, endDate]
    );
    return rows.map(row => ({
      date: row.date,
      totalCalories: row.totalCalories || 0,
      entryCount: row.entryCount || 0
    }));
  }

  static async addEntry(
    date: string,
    calories: number,
    foodName: string,
    note: string,
    timeLabel: string
  ): Promise<void> {
    const db = await getDatabase();
    const createdAt = new Date().toISOString();
    await db.runAsync(
      `INSERT INTO calorie_entries (date, calories, foodName, note, timeLabel, isFavorite, createdAt) 
       VALUES (?, ?, ?, ?, ?, 0, ?)`,
      [date, calories, foodName.trim(), note.trim(), timeLabel.trim(), createdAt]
    );
  }

  static async updateEntry(
    entry: CalorieEntry,
    calories: number,
    foodName: string,
    note: string,
    timeLabel: string
  ): Promise<void> {
    const db = await getDatabase();
    await db.runAsync(
      `UPDATE calorie_entries 
       SET calories = ?, foodName = ?, note = ?, timeLabel = ? 
       WHERE id = ?`,
      [calories, foodName.trim(), note.trim(), timeLabel.trim(), entry.id]
    );
  }

  static async addEntryFromPreset(date: string, preset: EntryQuickPreset): Promise<void> {
    await this.addEntry(
      date,
      preset.calories,
      preset.foodName,
      preset.note,
      preset.timeLabel
    );
  }

  static async updateEntryFavorite(entryId: number, isFavorite: boolean): Promise<void> {
    const db = await getDatabase();
    await db.runAsync(
      'UPDATE calorie_entries SET isFavorite = ? WHERE id = ?',
      [isFavorite ? 1 : 0, entryId]
    );
  }

  static async deleteEntry(entryId: number): Promise<void> {
    const db = await getDatabase();
    await db.runAsync(
      'DELETE FROM calorie_entries WHERE id = ?',
      [entryId]
    );
  }

  // Daily Condition Check Operations
  static async getDailyConditionCheck(date: string): Promise<DailyConditionCheck | null> {
    const db = await getDatabase();
    const row = await db.getFirstAsync<any>(
      'SELECT * FROM daily_condition_checks WHERE date = ?',
      [date]
    );
    if (!row) return null;
    return {
      date: row.date,
      bodyWeightKg: row.bodyWeightKg,
      proteinGrams: row.proteinGrams,
      resistanceSets: row.resistanceSets,
      mainLiftKg: row.mainLiftKg,
      relapseTrigger: row.relapseTrigger,
      copingAction: row.copingAction,
      sleepHours: row.sleepHours,
      fatigueScore: row.fatigueScore,
      hungerScore: row.hungerScore,
      moodScore: row.moodScore,
      workoutPerformanceScore: row.workoutPerformanceScore,
      updatedAt: row.updatedAt
    };
  }

  static async getDailyConditionChecks(startDate: string, endDate: string): Promise<DailyConditionCheck[]> {
    const db = await getDatabase();
    const rows = await db.getAllAsync<any>(
      'SELECT * FROM daily_condition_checks WHERE date BETWEEN ? AND ? ORDER BY date ASC',
      [startDate, endDate]
    );
    return rows.map(row => ({
      date: row.date,
      bodyWeightKg: row.bodyWeightKg,
      proteinGrams: row.proteinGrams,
      resistanceSets: row.resistanceSets,
      mainLiftKg: row.mainLiftKg,
      relapseTrigger: row.relapseTrigger,
      copingAction: row.copingAction,
      sleepHours: row.sleepHours,
      fatigueScore: row.fatigueScore,
      hungerScore: row.hungerScore,
      moodScore: row.moodScore,
      workoutPerformanceScore: row.workoutPerformanceScore,
      updatedAt: row.updatedAt
    }));
  }

  static async upsertDailyConditionCheck(
    date: string,
    bodyWeightKg: number | null,
    proteinGrams: number | null,
    resistanceSets: number | null,
    mainLiftKg: number | null,
    relapseTrigger: string | null,
    copingAction: string | null,
    sleepHours: number | null,
    fatigueScore: number | null,
    hungerScore: number | null,
    moodScore: number | null,
    workoutPerformanceScore: number | null
  ): Promise<void> {
    if (!this.hasAnyConditionSignal(
      bodyWeightKg,
      proteinGrams,
      resistanceSets,
      mainLiftKg,
      relapseTrigger,
      copingAction,
      sleepHours,
      fatigueScore,
      hungerScore,
      moodScore,
      workoutPerformanceScore
    )) {
      return;
    }

    const db = await getDatabase();
    const updatedAt = new Date().toISOString();
    
    const weightVal = bodyWeightKg && bodyWeightKg > 0 ? bodyWeightKg : null;
    const proteinVal = proteinGrams && proteinGrams > 0 ? proteinGrams : null;
    const setsVal = resistanceSets && resistanceSets > 0 ? resistanceSets : null;
    const liftVal = mainLiftKg && mainLiftKg > 0 ? mainLiftKg : null;
    const triggerVal = relapseTrigger && relapseTrigger.trim().length > 0 ? relapseTrigger.trim() : null;
    const actionVal = copingAction && copingAction.trim().length > 0 ? copingAction.trim() : null;
    const sleepVal = sleepHours && sleepHours > 0 ? sleepHours : null;
    const fatigueVal = fatigueScore && fatigueScore >= 1 && fatigueScore <= 5 ? fatigueScore : null;
    const hungerVal = hungerScore && hungerScore >= 1 && hungerScore <= 5 ? hungerScore : null;
    const moodVal = moodScore && moodScore >= 1 && moodScore <= 5 ? moodScore : null;
    const workoutVal = workoutPerformanceScore && workoutPerformanceScore >= 1 && workoutPerformanceScore <= 5 ? workoutPerformanceScore : null;

    await db.runAsync(
      `INSERT OR REPLACE INTO daily_condition_checks 
       (date, bodyWeightKg, proteinGrams, resistanceSets, mainLiftKg, relapseTrigger, copingAction, sleepHours, fatigueScore, hungerScore, moodScore, workoutPerformanceScore, updatedAt) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        date,
        weightVal,
        proteinVal,
        setsVal,
        liftVal,
        triggerVal,
        actionVal,
        sleepVal,
        fatigueVal,
        hungerVal,
        moodVal,
        workoutVal,
        updatedAt
      ]
    );
  }

  // Clear Database
  static async clearAllSavedData(): Promise<void> {
    const db = await getDatabase();
    await db.execAsync(`
      DELETE FROM calorie_entries;
      DELETE FROM daily_condition_checks;
      DELETE FROM mini_cut_plan;
    `);
  }

  // Helper Utilities
  private static hasAnyConditionSignal(
    bodyWeightKg: number | null,
    proteinGrams: number | null,
    resistanceSets: number | null,
    mainLiftKg: number | null,
    relapseTrigger: string | null,
    copingAction: string | null,
    sleepHours: number | null,
    fatigueScore: number | null,
    hungerScore: number | null,
    moodScore: number | null,
    workoutPerformanceScore: number | null
  ): boolean {
    return (
      (bodyWeightKg !== null && bodyWeightKg > 0) ||
      (proteinGrams !== null && proteinGrams > 0) ||
      (resistanceSets !== null && resistanceSets > 0) ||
      (mainLiftKg !== null && mainLiftKg > 0) ||
      (relapseTrigger !== null && relapseTrigger.trim().length > 0) ||
      (copingAction !== null && copingAction.trim().length > 0) ||
      (sleepHours !== null && sleepHours > 0) ||
      (fatigueScore !== null && fatigueScore > 0) ||
      (hungerScore !== null && hungerScore > 0) ||
      (moodScore !== null && moodScore > 0) ||
      (workoutPerformanceScore !== null && workoutPerformanceScore > 0)
    );
  }

  private static toQuickPresets(rows: any[], limit: number): EntryQuickPreset[] {
    const presets: EntryQuickPreset[] = [];
    const keys = new Set<string>();

    for (const row of rows) {
      const preset: EntryQuickPreset = {
        foodName: row.foodName,
        calories: row.calories,
        note: row.note,
        timeLabel: row.timeLabel,
        isFavorite: row.isFavorite === 1
      };
      const key = `${preset.foodName}|${preset.calories}|${preset.note}|${preset.timeLabel}`;
      if (!keys.has(key)) {
        keys.add(key);
        presets.push(preset);
      }
      if (presets.length >= limit) break;
    }
    return presets;
  }
}
