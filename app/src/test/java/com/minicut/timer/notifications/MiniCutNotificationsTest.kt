package com.minicut.timer.notifications

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun nextTriggerMillis_skipsWeekendWhenFridayWeekdayReminderAlreadyPassed() {
        val now = ZonedDateTime.of(2026, 4, 10, 20, 0, 0, 0, ZoneId.of("Asia/Seoul"))

        val trigger = nextTriggerMillis(19, 0, cadence = ReminderCadence.Weekdays, now = now)

        assertEquals(
            ZonedDateTime.of(2026, 4, 13, 19, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
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

    @Test
    fun reminderScheduleRequests_includesOnlyEnabledSlotsWithConfiguredTimes() {
        val settings =
            NotificationSettings(
                cadence = ReminderCadence.Weekdays,
                morning = ReminderSetting(enabled = false, time = ReminderTime(8, 30)),
                evening = ReminderSetting(enabled = true, time = ReminderTime(21, 15)),
            )

        val requests = reminderScheduleRequests(settings)

        assertEquals(
            listOf(
                ReminderScheduleRequest(
                    slot = ReminderSlot.Evening,
                    time = ReminderTime(21, 15),
                    cadence = ReminderCadence.Weekdays,
                ),
            ),
            requests,
        )
    }

    @Test
    fun reminderScheduleRequests_keepsBothDefaultSlotsWhenEnabled() {
        val requests = reminderScheduleRequests(NotificationSettings())

        assertEquals(
            listOf(
                ReminderScheduleRequest(
                    slot = ReminderSlot.Morning,
                    time = ReminderSlot.Morning.defaultTime,
                    cadence = ReminderCadence.Daily,
                ),
                ReminderScheduleRequest(
                    slot = ReminderSlot.Evening,
                    time = ReminderSlot.Evening.defaultTime,
                    cadence = ReminderCadence.Daily,
                ),
            ),
            requests,
        )
    }

    @Test
    fun shouldSkipReminder_skipsDisabledSlot() {
        val now = ZonedDateTime.of(2026, 4, 13, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

        assertTrue(
            shouldSkipReminder(
                slotSetting = ReminderSetting(enabled = false, time = ReminderSlot.Morning.defaultTime),
                settings = NotificationSettings(cadence = ReminderCadence.Daily),
                now = now,
            ),
        )
    }

    @Test
    fun shouldSkipReminder_skipsWeekendOnlyForWeekdayCadence() {
        val saturday = ZonedDateTime.of(2026, 4, 11, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        val enabledMorning = ReminderSetting(enabled = true, time = ReminderSlot.Morning.defaultTime)

        assertTrue(
            shouldSkipReminder(
                slotSetting = enabledMorning,
                settings = NotificationSettings(cadence = ReminderCadence.Weekdays),
                now = saturday,
            ),
        )
        assertFalse(
            shouldSkipReminder(
                slotSetting = enabledMorning,
                settings = NotificationSettings(cadence = ReminderCadence.Daily),
                now = saturday,
            ),
        )
    }
}
