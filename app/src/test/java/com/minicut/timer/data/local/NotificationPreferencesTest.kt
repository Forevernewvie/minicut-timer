package com.minicut.timer.data.local

import com.minicut.timer.notifications.ReminderCadence
import com.minicut.timer.notifications.ReminderSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPreferencesTest {
    @Test
    fun sanitizeCadence_fallsBackToDailyForUnknownValues() {
        assertEquals(ReminderCadence.Daily, sanitizeCadence("NotACadence"))
        assertEquals(ReminderCadence.Daily, sanitizeCadence(null))
    }

    @Test
    fun sanitizeCadence_keepsKnownValues() {
        assertEquals(ReminderCadence.Weekdays, sanitizeCadence(ReminderCadence.Weekdays.name))
    }

    @Test
    fun sanitizeReminderTime_fallsBackToDefaultWhenPersistedValueIsOutOfRange() {
        val safeTime =
            sanitizeReminderTime(
                hourOfDay = 99,
                minute = -1,
                defaultTime = ReminderSlot.Morning.defaultTime,
            )

        assertEquals(ReminderSlot.Morning.defaultTime, safeTime)
    }

    @Test
    fun sanitizeReminderTime_preservesValidPersistedValue() {
        val safeTime =
            sanitizeReminderTime(
                hourOfDay = 6,
                minute = 45,
                defaultTime = ReminderSlot.Evening.defaultTime,
            )

        assertEquals(6, safeTime.hourOfDay)
        assertEquals(45, safeTime.minute)
    }
}
