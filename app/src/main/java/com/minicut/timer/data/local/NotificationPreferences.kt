package com.minicut.timer.data.local

import android.content.Context
import com.minicut.timer.notifications.NotificationSettings
import com.minicut.timer.notifications.ReminderCadence
import com.minicut.timer.notifications.ReminderSetting
import com.minicut.timer.notifications.ReminderSlot
import com.minicut.timer.notifications.ReminderTime

object NotificationPreferences {
    private const val PREFS_NAME = "minicut_prefs"
    private const val KEY_CADENCE = "notification_cadence"
    private const val KEY_MORNING_ENABLED = "notification_morning_enabled"
    private const val KEY_MORNING_HOUR = "notification_morning_hour"
    private const val KEY_MORNING_MINUTE = "notification_morning_minute"
    private const val KEY_EVENING_ENABLED = "notification_evening_enabled"
    private const val KEY_EVENING_HOUR = "notification_evening_hour"
    private const val KEY_EVENING_MINUTE = "notification_evening_minute"

    fun load(context: Context): NotificationSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return NotificationSettings(
            cadence = sanitizeCadence(prefs.getString(KEY_CADENCE, ReminderCadence.Daily.name)),
            morning =
                ReminderSetting(
                    enabled = prefs.getBoolean(KEY_MORNING_ENABLED, true),
                    time = sanitizeReminderTime(
                        hourOfDay = prefs.getInt(KEY_MORNING_HOUR, ReminderSlot.Morning.defaultTime.hourOfDay),
                        minute = prefs.getInt(KEY_MORNING_MINUTE, ReminderSlot.Morning.defaultTime.minute),
                        defaultTime = ReminderSlot.Morning.defaultTime,
                    ),
                ),
            evening =
                ReminderSetting(
                    enabled = prefs.getBoolean(KEY_EVENING_ENABLED, true),
                    time = sanitizeReminderTime(
                        hourOfDay = prefs.getInt(KEY_EVENING_HOUR, ReminderSlot.Evening.defaultTime.hourOfDay),
                        minute = prefs.getInt(KEY_EVENING_MINUTE, ReminderSlot.Evening.defaultTime.minute),
                        defaultTime = ReminderSlot.Evening.defaultTime,
                    ),
                ),
        )
    }

    fun save(
        context: Context,
        settings: NotificationSettings,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CADENCE, settings.cadence.name)
            .putBoolean(KEY_MORNING_ENABLED, settings.morning.enabled)
            .putInt(KEY_MORNING_HOUR, settings.morning.time.hourOfDay)
            .putInt(KEY_MORNING_MINUTE, settings.morning.time.minute)
            .putBoolean(KEY_EVENING_ENABLED, settings.evening.enabled)
            .putInt(KEY_EVENING_HOUR, settings.evening.time.hourOfDay)
            .putInt(KEY_EVENING_MINUTE, settings.evening.time.minute)
            .apply()
    }
}

internal fun sanitizeCadence(name: String?): ReminderCadence =
    name
        ?.let { stored -> ReminderCadence.entries.firstOrNull { it.name == stored } }
        ?: ReminderCadence.Daily

internal fun sanitizeReminderTime(
    hourOfDay: Int,
    minute: Int,
    defaultTime: ReminderTime,
): ReminderTime =
    runCatching {
        ReminderTime(
            hourOfDay = hourOfDay,
            minute = minute,
        )
    }.getOrDefault(defaultTime)
