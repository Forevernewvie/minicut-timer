package com.minicut.timer.ui.util

import com.minicut.timer.domain.model.CalorieRangeStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
private val compactDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
private val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)

fun Int.asKcal(): String = "${NumberFormat.getIntegerInstance(Locale.KOREAN).format(this)} kcal"

fun Int.asCompactKcal(): String =
    if (this >= 1000) {
        String.format(Locale.KOREAN, "%.1fk", this / 1000.0)
    } else {
        NumberFormat.getIntegerInstance(Locale.KOREAN).format(this)
    }

fun LocalDate.asDisplayDate(): String = format(dateFormatter)

fun LocalDate.asCompactDate(): String = format(compactDateFormatter)

fun YearMonth.asDisplayMonth(): String = format(monthFormatter)

fun String.asMealHeadline(): String = ifBlank { "먹은 음식 메모 없음" }

fun CalorieRangeStatus.asLabel(): String =
    when (this) {
        CalorieRangeStatus.NoData -> "기록 없음"
        CalorieRangeStatus.Below -> "권장보다 낮아요"
        CalorieRangeStatus.InRange -> "권장 범위"
        CalorieRangeStatus.Above -> "권장보다 높아요"
    }
