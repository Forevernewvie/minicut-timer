import * as SQLite from 'expo-sqlite';

let dbInstance: SQLite.SQLiteDatabase | null = null;

export async function getDatabase(): Promise<SQLite.SQLiteDatabase> {
  if (dbInstance) return dbInstance;
  
  dbInstance = await SQLite.openDatabaseAsync('minicut.db');
  
  // Enable WAL and create tables
  await dbInstance.execAsync(`
    PRAGMA journal_mode = WAL;
    
    CREATE TABLE IF NOT EXISTS mini_cut_plan (
      id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
      startDate TEXT NOT NULL,
      durationWeeks INTEGER NOT NULL,
      endDate TEXT NOT NULL,
      dailyTargetKcal INTEGER NOT NULL,
      goalMode TEXT NOT NULL,
      activityLevel TEXT NOT NULL,
      estimatedMaintenanceKcal INTEGER NOT NULL,
      isActive INTEGER NOT NULL DEFAULT 1
    );

    CREATE TABLE IF NOT EXISTS calorie_entries (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      date TEXT NOT NULL,
      calories INTEGER NOT NULL,
      foodName TEXT NOT NULL,
      note TEXT NOT NULL,
      timeLabel TEXT NOT NULL,
      isFavorite INTEGER NOT NULL DEFAULT 0,
      createdAt TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_calorie_entries_date ON calorie_entries(date);

    CREATE TABLE IF NOT EXISTS daily_condition_checks (
      date TEXT PRIMARY KEY,
      bodyWeightKg REAL,
      proteinGrams INTEGER,
      resistanceSets INTEGER,
      mainLiftKg REAL,
      relapseTrigger TEXT,
      copingAction TEXT,
      sleepHours REAL,
      fatigueScore INTEGER,
      hungerScore INTEGER,
      moodScore INTEGER,
      workoutPerformanceScore INTEGER,
      updatedAt TEXT NOT NULL
    );
  `);
  
  return dbInstance;
}
