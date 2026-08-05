package com.minicut.timer.data.local.query

import com.minicut.timer.domain.model.DailyCalorieSummary
import java.time.LocalDate

data class DailyCalorieSummaryRow(
    val dateEpochDay: Long,
    val totalCalories: Int,
    val entryCount: Int,
)

fun DailyCalorieSummaryRow.toDomain(): DailyCalorieSummary =
    DailyCalorieSummary(
        date = LocalDate.ofEpochDay(dateEpochDay),
        totalCalories = totalCalories,
        entryCount = entryCount,
    )
