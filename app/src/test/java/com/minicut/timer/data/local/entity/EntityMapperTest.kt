package com.minicut.timer.data.local.entity

import com.minicut.timer.data.local.query.DailyCalorieSummaryRow
import com.minicut.timer.data.local.query.toDomain
import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPlan
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class EntityMapperTest {

    @Test
    fun miniCutPlan_entityAndDomainMapping_roundTripsValues() {
        val domain =
            MiniCutPlan(
                startDate = LocalDate.of(2026, 4, 10),
                durationWeeks = 4,
                endDate = LocalDate.of(2026, 5, 7),
                dailyTargetKcal = 1300,
                goalMode = MiniCutGoalMode.EventReady,
                activityLevel = ActivityLevel.High,
                estimatedMaintenanceKcal = 2500,
                isActive = true,
            )

        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(domain.startDate, mappedBack.startDate)
        assertEquals(domain.durationWeeks, mappedBack.durationWeeks)
        assertEquals(domain.endDate, mappedBack.endDate)
        assertEquals(domain.dailyTargetKcal, mappedBack.dailyTargetKcal)
        assertEquals(domain.goalMode, mappedBack.goalMode)
        assertEquals(domain.activityLevel, mappedBack.activityLevel)
        assertEquals(domain.estimatedMaintenanceKcal, mappedBack.estimatedMaintenanceKcal)
        assertEquals(domain.isActive, mappedBack.isActive)
    }

    @Test
    fun calorieEntry_entityAndDomainMapping_roundTripsValues() {
        val createdAt = LocalDateTime.of(2026, 4, 10, 12, 34, 56)
        val domain =
            CalorieEntry(
                id = 9L,
                date = LocalDate.of(2026, 4, 10),
                calories = 480,
                foodName = "닭가슴살",
                note = "운동 후",
                timeLabel = "점심",
                createdAt = createdAt,
            )

        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.date.toEpochDay(), entity.dateEpochDay)
        assertEquals(
            createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            entity.createdAtEpochMillis,
        )
        assertEquals(domain, mappedBack)
    }

    @Test
    fun dailySummaryRow_mapsToDomain() {
        val row =
            DailyCalorieSummaryRow(
                dateEpochDay = LocalDate.of(2026, 4, 10).toEpochDay(),
                totalCalories = 1220,
                entryCount = 3,
            )

        val domain = row.toDomain()

        assertEquals(LocalDate.of(2026, 4, 10), domain.date)
        assertEquals(1220, domain.totalCalories)
        assertEquals(3, domain.entryCount)
    }

    @Test
    fun dailyConditionCheck_entityAndDomainMapping_roundTripsValues() {
        val domain =
            DailyConditionCheck(
                date = LocalDate.of(2026, 4, 10),
                bodyWeightKg = 78.6f,
                proteinGrams = 160,
                resistanceSets = 12,
                mainLiftKg = 105f,
                sleepHours = 7.5f,
                fatigueScore = 2,
                hungerScore = 3,
                moodScore = 4,
                workoutPerformanceScore = 4,
                updatedAt = LocalDateTime.of(2026, 4, 10, 20, 30),
            )

        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(domain.date.toEpochDay(), entity.dateEpochDay)
        assertEquals(domain.bodyWeightKg, mappedBack.bodyWeightKg)
        assertEquals(domain.proteinGrams, mappedBack.proteinGrams)
        assertEquals(domain.resistanceSets, mappedBack.resistanceSets)
        assertEquals(domain.mainLiftKg, mappedBack.mainLiftKg)
        assertEquals(domain.sleepHours, mappedBack.sleepHours)
        assertEquals(domain.fatigueScore, mappedBack.fatigueScore)
        assertEquals(domain.hungerScore, mappedBack.hungerScore)
        assertEquals(domain.moodScore, mappedBack.moodScore)
        assertEquals(domain.workoutPerformanceScore, mappedBack.workoutPerformanceScore)
    }
}
