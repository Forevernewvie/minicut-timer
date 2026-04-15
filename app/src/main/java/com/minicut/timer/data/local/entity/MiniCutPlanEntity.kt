package com.minicut.timer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minicut.timer.domain.model.MiniCutPlan
import java.time.LocalDate

@Entity(tableName = "mini_cut_plan")
data class MiniCutPlanEntity(
    @PrimaryKey val id: Int = 1,
    val startDateEpochDay: Long,
    val durationWeeks: Int,
    val endDateEpochDay: Long,
    val dailyTargetKcal: Int,
    val isActive: Boolean = true,
)

fun MiniCutPlanEntity.toDomain(): MiniCutPlan =
    MiniCutPlan(
        startDate = LocalDate.ofEpochDay(startDateEpochDay),
        durationWeeks = durationWeeks,
        endDate = LocalDate.ofEpochDay(endDateEpochDay),
        dailyTargetKcal = dailyTargetKcal,
        isActive = isActive,
    )

fun MiniCutPlan.toEntity(): MiniCutPlanEntity =
    MiniCutPlanEntity(
        startDateEpochDay = startDate.toEpochDay(),
        durationWeeks = durationWeeks,
        endDateEpochDay = endDate.toEpochDay(),
        dailyTargetKcal = dailyTargetKcal,
        isActive = isActive,
    )
