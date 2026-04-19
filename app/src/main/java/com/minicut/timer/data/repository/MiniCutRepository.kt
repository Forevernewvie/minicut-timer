package com.minicut.timer.data.repository

import com.minicut.timer.data.local.dao.CalorieEntryDao
import com.minicut.timer.data.local.dao.DailyConditionCheckDao
import com.minicut.timer.data.local.dao.MiniCutPlanDao
import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.DailyConditionCheckEntity
import com.minicut.timer.data.local.entity.toDomain
import com.minicut.timer.data.local.entity.toEntity
import com.minicut.timer.data.local.query.toDomain
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.DailyCalorieSummary
import com.minicut.timer.domain.model.EntryQuickPreset
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPlan
import com.minicut.timer.domain.rules.MiniCutRules
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MiniCutRepository(
    private val planDao: MiniCutPlanDao,
    private val calorieEntryDao: CalorieEntryDao,
    private val dailyConditionCheckDao: DailyConditionCheckDao,
) {
    fun observePlan(): Flow<MiniCutPlan?> =
        planDao.observePlan().map { it?.toDomain() }

    fun observeEntriesForDate(date: LocalDate): Flow<List<CalorieEntry>> =
        calorieEntryDao.observeEntriesForDate(date.toEpochDay()).map { list ->
            list.map { it.toDomain() }
        }

    fun observeTodayTotal(today: LocalDate = LocalDate.now()): Flow<Int> =
        calorieEntryDao.observeTotalForDate(today.toEpochDay())

    fun observeRecentEntryPresets(limit: Int = 4): Flow<List<EntryQuickPreset>> =
        calorieEntryDao.observeRecentEntries(limit = limit * 3).map { entries ->
            entries.toQuickPresets(limit = limit)
        }

    fun observeFavoriteEntryPresets(limit: Int = 4): Flow<List<EntryQuickPreset>> =
        calorieEntryDao.observeFavoriteEntries(limit = limit * 3).map { entries ->
            entries
                .toQuickPresets(limit = limit)
                .map { it.copy(isFavorite = true) }
        }

    fun observeDailySummaries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCalorieSummary>> =
        calorieEntryDao.observeDailySummaries(
            startEpochDay = startDate.toEpochDay(),
            endEpochDay = endDate.toEpochDay(),
        ).map { rows ->
            rows.map { it.toDomain() }
        }

    fun observeDailyConditionCheck(date: LocalDate): Flow<DailyConditionCheck?> =
        dailyConditionCheckDao.observeForDate(date.toEpochDay()).map { it?.toDomain() }

    fun observeDailyConditionChecks(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyConditionCheck>> =
        dailyConditionCheckDao.observeInRange(
            startEpochDay = startDate.toEpochDay(),
            endEpochDay = endDate.toEpochDay(),
        ).map { rows ->
            rows.map { it.toDomain() }
        }

    suspend fun savePlan(
        startDate: LocalDate,
        durationWeeks: Int,
        dailyTargetKcal: Int,
        goalMode: MiniCutGoalMode = MiniCutGoalMode.MassReset,
    ) {
        val endDate = MiniCutRules.calculateEndDate(startDate, durationWeeks)
        require(MiniCutRules.isValidTarget(dailyTargetKcal)) {
            "하루 목표 칼로리는 ${MiniCutRules.RECOMMENDED_MIN_KCAL}~${MiniCutRules.RECOMMENDED_MAX_KCAL} 범위여야 합니다."
        }
        planDao.upsert(
            MiniCutPlan(
                startDate = startDate,
                durationWeeks = durationWeeks,
                endDate = endDate,
                dailyTargetKcal = dailyTargetKcal,
                goalMode = goalMode,
                isActive = true,
            ).toEntity(),
        )
    }

    suspend fun addEntry(
        date: LocalDate,
        calories: Int,
        foodName: String,
        note: String,
        timeLabel: String,
    ) {
        calorieEntryDao.insert(
            CalorieEntryEntity(
                dateEpochDay = date.toEpochDay(),
                calories = calories,
                foodName = foodName.trim(),
                note = note.trim(),
                timeLabel = timeLabel.trim(),
                createdAtEpochMillis = LocalDateTime.now()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
            ),
        )
    }

    suspend fun updateEntry(
        entry: CalorieEntry,
        calories: Int,
        foodName: String,
        note: String,
        timeLabel: String,
    ) {
        calorieEntryDao.update(
            entry.copy(
                calories = calories,
                foodName = foodName.trim(),
                note = note.trim(),
                timeLabel = timeLabel.trim(),
            ).toEntity(),
        )
    }

    suspend fun addEntryFromPreset(
        date: LocalDate,
        preset: EntryQuickPreset,
    ) {
        addEntry(
            date = date,
            calories = preset.calories,
            foodName = preset.foodName,
            note = preset.note,
            timeLabel = preset.timeLabel,
        )
    }

    suspend fun updateEntryFavorite(
        entryId: Long,
        isFavorite: Boolean,
    ) {
        calorieEntryDao.updateFavorite(entryId = entryId, isFavorite = isFavorite)
    }

    suspend fun deleteEntry(entryId: Long) {
        calorieEntryDao.deleteById(entryId)
    }

    suspend fun clearAllSavedData() {
        calorieEntryDao.deleteAll()
        dailyConditionCheckDao.deleteAll()
        planDao.deletePlan()
    }

    suspend fun upsertDailyConditionCheck(
        date: LocalDate,
        bodyWeightKg: Float?,
        proteinGrams: Int?,
        resistanceSets: Int?,
    ) {
        if ((bodyWeightKg ?: 0f) <= 0f && (proteinGrams ?: 0) <= 0 && (resistanceSets ?: 0) <= 0) {
            return
        }
        dailyConditionCheckDao.upsert(
            DailyConditionCheckEntity(
                dateEpochDay = date.toEpochDay(),
                bodyWeightKg = bodyWeightKg?.takeIf { it > 0f },
                proteinGrams = proteinGrams?.takeIf { it > 0 },
                resistanceSets = resistanceSets?.takeIf { it > 0 },
                updatedAtEpochMillis = LocalDateTime.now()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
            ),
        )
    }

    private fun List<CalorieEntryEntity>.toQuickPresets(limit: Int): List<EntryQuickPreset> =
        asSequence()
            .map { entity ->
                EntryQuickPreset(
                    foodName = entity.foodName,
                    calories = entity.calories,
                    note = entity.note,
                    timeLabel = entity.timeLabel,
                    isFavorite = entity.isFavorite,
                )
            }.distinctBy { preset ->
                listOf(preset.foodName, preset.calories, preset.note, preset.timeLabel).joinToString("|")
            }.take(limit)
            .toList()
}
