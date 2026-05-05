package com.minicut.timer.notifications

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniCutNotificationsTest {

    @Test
    fun nextTriggerMillis_returnsSameDayForFutureTime() {
        val now = ZonedDateTime.of(2026, 4, 10, 9, 15, 0, 0, ZoneId.of("Asia/Seoul"))

        val trigger = nextTriggerMillis(10, 0, now = now)

        assertEquals(
            ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun nextTriggerMillis_rollsToNextDayWhenTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 4, 10, 20, 0, 0, 0, ZoneId.of("Asia/Seoul"))

        val trigger = nextTriggerMillis(19, 0, now = now)

        assertEquals(
            ZonedDateTime.of(2026, 4, 11, 19, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun nextTriggerMillis_rollsToNextDayWhenTimeMatchesExactly() {
        val now = ZonedDateTime.of(2026, 4, 10, 19, 0, 0, 0, ZoneId.of("Asia/Seoul"))

        val trigger = nextTriggerMillis(19, 0, now = now)

        assertEquals(
            ZonedDateTime.of(2026, 4, 11, 19, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun nextTriggerMillis_skipsWeekendForWeekdayCadence() {
        val now = ZonedDateTime.of(2026, 4, 11, 8, 0, 0, 0, ZoneId.of("Asia/Seoul"))

        val trigger = nextTriggerMillis(10, 0, cadence = ReminderCadence.Weekdays, now = now)

        assertEquals(
            ZonedDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun nextTriggerMillis_keepsWeekdayWhenFutureTimeIsStillToday() {
        val now = ZonedDateTime.of(2026, 4, 13, 7, 30, 0, 0, ZoneId.of("Asia/Seoul"))

        val trigger = nextTriggerMillis(10, 0, cadence = ReminderCadence.Weekdays, now = now)

        assertEquals(
            ZonedDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun fallbackReminderMessage_usesExistingSlotRotationWhenRepositoryUnavailable() {
        val now = ZonedDateTime.of(2026, 4, 10, 7, 30, 0, 0, ZoneId.of("Asia/Seoul"))

        val message = fallbackReminderMessage(ReminderSlot.Morning, now)

        assertEquals(ReminderSlot.Morning.messages[now.dayOfMonth % ReminderSlot.Morning.messages.size], message)
    }
}
