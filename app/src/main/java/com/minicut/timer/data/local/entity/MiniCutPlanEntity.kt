package com.minicut.timer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPlan
import java.time.LocalDate

@Entity(tableName = "mini_cut_plan")
data class MiniCutPlanEntity(
    @PrimaryKey val id: Int = 1,
    val startDateEpochDay: Long,
    val durationWeeks: Int,
    val endDateEpochDay: Long,
    val dailyTargetKcal: Int,
    val goalMode: String = MiniCutGoalMode.MassReset.name,
    val activityLevel: String = ActivityLevel.Moderate.name,
    val estimatedMaintenanceKcal: Int = 0,
    val isActive: Boolean = true,
)

fun MiniCutPlanEntity.toDomain(): MiniCutPlan =
    MiniCutPlan(
        startDate = LocalDate.ofEpochDay(startDateEpochDay),
        durationWeeks = durationWeeks,
        endDate = LocalDate.ofEpochDay(endDateEpochDay),
        dailyTargetKcal = dailyTargetKcal,
        goalMode = goalMode.toGoalMode(),
        activityLevel = activityLevel.toActivityLevel(),
        estimatedMaintenanceKcal = estimatedMaintenanceKcal,
        isActive = isActive,
    )

fun MiniCutPlan.toEntity(): MiniCutPlanEntity =
    MiniCutPlanEntity(
        startDateEpochDay = startDate.toEpochDay(),
        durationWeeks = durationWeeks,
        endDateEpochDay = endDate.toEpochDay(),
        dailyTargetKcal = dailyTargetKcal,
        goalMode = goalMode.name,
        activityLevel = activityLevel.name,
        estimatedMaintenanceKcal = estimatedMaintenanceKcal,
        isActive = isActive,
    )

private fun String.toGoalMode(): MiniCutGoalMode =
    runCatching { MiniCutGoalMode.valueOf(this) }
        .getOrDefault(MiniCutGoalMode.MassReset)

private fun String.toActivityLevel(): ActivityLevel =
    runCatching { ActivityLevel.valueOf(this) }
        .getOrDefault(ActivityLevel.Moderate)
