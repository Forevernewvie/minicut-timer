package com.minicut.timer.ui.util

import com.minicut.timer.domain.model.CalorieRangeStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class UiFormattersTest {

    @Test
    fun calorieFormatting_andLabels_matchUserFacingCopy() {
        assertEquals("0 kcal", 0.asKcal())
        assertEquals("999 kcal", 999.asKcal())
        assertEquals("999", 999.asCompactKcal())
        assertEquals("1.5k", 1_500.asCompactKcal())
        assertEquals("먹은 음식 메모 없음", "".asMealHeadline())
        assertEquals("닭가슴살", "닭가슴살".asMealHeadline())
        assertEquals("기록 없음", CalorieRangeStatus.NoData.asLabel())
        assertEquals("권장보다 낮아요", CalorieRangeStatus.Below.asLabel())
        assertEquals("권장 범위", CalorieRangeStatus.InRange.asLabel())
        assertEquals("권장보다 높아요", CalorieRangeStatus.Above.asLabel())
    }

    @Test
    fun dateFormatting_usesKoreanPresentation() {
        assertEquals("4월 10일 (금)", LocalDate.of(2026, 4, 10).asDisplayDate())
        assertEquals("2026.04.10", LocalDate.of(2026, 4, 10).asCompactDate())
        assertEquals("2026년 4월", YearMonth.of(2026, 4).asDisplayMonth())
    }
}
