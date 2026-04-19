package com.minicut.timer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minicut.timer.domain.model.DailyConditionCheck
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "daily_condition_checks")
data class DailyConditionCheckEntity(
    @PrimaryKey val dateEpochDay: Long,
    val bodyWeightKg: Float? = null,
    val proteinGrams: Int? = null,
    val resistanceSets: Int? = null,
    val updatedAtEpochMillis: Long,
)

fun DailyConditionCheckEntity.toDomain(): DailyConditionCheck =
    DailyConditionCheck(
        date = LocalDate.ofEpochDay(dateEpochDay),
        bodyWeightKg = bodyWeightKg,
        proteinGrams = proteinGrams,
        resistanceSets = resistanceSets,
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAtEpochMillis), ZoneId.systemDefault()),
    )

fun DailyConditionCheck.toEntity(): DailyConditionCheckEntity =
    DailyConditionCheckEntity(
        dateEpochDay = date.toEpochDay(),
        bodyWeightKg = bodyWeightKg,
        proteinGrams = proteinGrams,
        resistanceSets = resistanceSets,
        updatedAtEpochMillis = updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
