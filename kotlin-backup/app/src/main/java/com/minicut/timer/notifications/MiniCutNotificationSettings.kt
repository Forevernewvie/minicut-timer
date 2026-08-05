package com.minicut.timer.notifications

import java.time.DayOfWeek

enum class ReminderCadence(
    val displayName: String,
) {
    Daily("매일"),
    Weekdays("평일만"),
}

data class ReminderTime(
    val hourOfDay: Int,
    val minute: Int,
) {
    init {
        require(hourOfDay in 0..23) { "hourOfDay must be 0..23" }
        require(minute in 0..59) { "minute must be 0..59" }
    }

    fun formatted(): String = "%02d:%02d".format(hourOfDay, minute)
}

data class ReminderSetting(
    val enabled: Boolean = true,
    val time: ReminderTime,
)

enum class ReminderSlot(
    val requestCode: Int,
    val displayName: String,
    val title: String,
    val messages: List<String>,
    val defaultTime: ReminderTime,
) {
    Morning(
        requestCode = 1001,
        displayName = "오전",
        title = "오전 미니컷 체크인",
        messages = listOf(
            "오늘도 짧고 선명하게. 첫 식사부터 가볍게 기록해보세요.",
            "완벽함보다 꾸준함이 중요해요. 오전 흐름만 잡아도 충분합니다.",
            "미니컷은 짧게, 판단은 또렷하게. 지금 상태를 기록해보세요.",
        ),
        defaultTime = ReminderTime(hourOfDay = 10, minute = 0),
    ),
    Evening(
        requestCode = 1002,
        displayName = "저녁",
        title = "저녁 미니컷 체크인",
        messages = listOf(
            "오늘 하루도 거의 끝났어요. 남은 칼로리와 식사를 확인해보세요.",
            "짧은 미니컷일수록 마무리가 중요해요. 오늘 기록을 정리해보세요.",
            "지금 확인하면 내일이 더 쉬워집니다. 오늘 섭취를 마감해보세요.",
        ),
        defaultTime = ReminderTime(hourOfDay = 19, minute = 0),
    ),
}

data class NotificationSettings(
    val cadence: ReminderCadence = ReminderCadence.Daily,
    val morning: ReminderSetting = ReminderSetting(time = ReminderSlot.Morning.defaultTime),
    val evening: ReminderSetting = ReminderSetting(time = ReminderSlot.Evening.defaultTime),
) {
    fun settingFor(slot: ReminderSlot): ReminderSetting =
        when (slot) {
            ReminderSlot.Morning -> morning
            ReminderSlot.Evening -> evening
        }

    fun updateSlot(
        slot: ReminderSlot,
        transform: (ReminderSetting) -> ReminderSetting,
    ): NotificationSettings =
        when (slot) {
            ReminderSlot.Morning -> copy(morning = transform(morning))
            ReminderSlot.Evening -> copy(evening = transform(evening))
        }
}

internal fun DayOfWeek.isWeekend(): Boolean =
    this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY
