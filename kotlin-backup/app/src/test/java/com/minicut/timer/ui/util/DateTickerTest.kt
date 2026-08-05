package com.minicut.timer.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTickerTest {

    @Test
    fun millisUntilNextDay_returnsDelayUntilFollowingMidnightPlusBuffer() {
        val now = ZonedDateTime.of(2026, 4, 10, 23, 59, 30, 0, ZoneId.of("Asia/Seoul"))

        val delay = millisUntilNextDay(now)

        assertEquals(31_000L, delay)
    }
}
