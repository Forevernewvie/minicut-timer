package com.minicut.timer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.minicut.timer.domain.model.CalorieEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "calorie_entries",
    indices = [Index(value = ["dateEpochDay"])],
)
data class CalorieEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val calories: Int,
    val foodName: String,
    val note: String,
    val timeLabel: String,
    val isFavorite: Boolean = false,
    val createdAtEpochMillis: Long,
)

fun CalorieEntryEntity.toDomain(): CalorieEntry =
    CalorieEntry(
        id = id,
        date = LocalDate.ofEpochDay(dateEpochDay),
        calories = calories,
        foodName = foodName,
        note = note,
        timeLabel = timeLabel,
        isFavorite = isFavorite,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAtEpochMillis),
            ZoneId.systemDefault(),
        ),
    )

fun CalorieEntry.toEntity(): CalorieEntryEntity =
    CalorieEntryEntity(
        id = id,
        dateEpochDay = date.toEpochDay(),
        calories = calories,
        foodName = foodName,
        note = note,
        timeLabel = timeLabel,
        isFavorite = isFavorite,
        createdAtEpochMillis = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
