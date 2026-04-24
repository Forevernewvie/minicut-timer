package com.minicut.timer.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.minicut.timer.MainActivity
import com.minicut.timer.R
import com.minicut.timer.data.local.NotificationPreferences
import com.minicut.timer.util.MiniCutDiagnostics
import java.time.ZonedDateTime

private const val CHANNEL_ID = "minicut_encouragements"
private const val CHANNEL_NAME = "미니컷 격려 알림"
private const val CHANNEL_DESCRIPTION = "미니컷 진행을 위한 오전/저녁 격려 메시지"
private const val EXTRA_SLOT = "slot"

fun createMiniCutNotificationChannel(context: Context) {
    MiniCutDiagnostics.guard("MiniCutNotifications.createChannel") {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@guard

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
        manager.createNotificationChannel(channel)
    }
}

fun scheduleMiniCutNotifications(context: Context) {
    MiniCutDiagnostics.guard("MiniCutNotifications.schedule") {
        syncMiniCutNotifications(context)
    }
}

fun syncMiniCutNotifications(
    context: Context,
    settings: NotificationSettings = NotificationPreferences.load(context),
) {
    createMiniCutNotificationChannel(context)
    ReminderSlot.entries.forEach { cancelSlot(context, it) }
    ReminderSlot.entries.forEach { slot ->
        val slotSetting = settings.settingFor(slot)
        if (slotSetting.enabled) {
            scheduleSlot(
                context = context,
                slot = slot,
                time = slotSetting.time,
                cadence = settings.cadence,
            )
        }
    }
}

private fun scheduleSlot(
    context: Context,
    slot: ReminderSlot,
    time: ReminderTime,
    cadence: ReminderCadence,
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = pendingIntentFor(context, slot)
    val nextTrigger = nextTriggerMillis(time.hourOfDay, time.minute, cadence)
    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        nextTrigger,
        pendingIntent,
    )
}

private fun cancelSlot(
    context: Context,
    slot: ReminderSlot,
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(pendingIntentFor(context, slot))
}

private fun pendingIntentFor(
    context: Context,
    slot: ReminderSlot,
): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        slot.requestCode,
        Intent(context, MiniCutNotificationReceiver::class.java).putExtra(EXTRA_SLOT, slot.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

internal fun nextTriggerMillis(
    hourOfDay: Int,
    minute: Int,
    cadence: ReminderCadence = ReminderCadence.Daily,
    now: ZonedDateTime = ZonedDateTime.now(),
): Long {
    var trigger =
        now.withHour(hourOfDay)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
    if (!trigger.isAfter(now)) {
        trigger = trigger.plusDays(1)
    }
    if (cadence == ReminderCadence.Weekdays) {
        while (trigger.dayOfWeek.isWeekend()) {
            trigger = trigger.plusDays(1)
        }
    }
    return trigger.toInstant().toEpochMilli()
}

class MiniCutNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MiniCutDiagnostics.guard("MiniCutNotificationReceiver.onReceive") {
            val slotName = intent.getStringExtra(EXTRA_SLOT) ?: return@guard
            val slot = ReminderSlot.entries.firstOrNull { it.name == slotName } ?: return@guard
            val settings = NotificationPreferences.load(context)
            val slotSetting = settings.settingFor(slot)
            val now = ZonedDateTime.now()

            if (!slotSetting.enabled ||
                (settings.cadence == ReminderCadence.Weekdays && now.dayOfWeek.isWeekend())
            ) {
                syncMiniCutNotifications(context, settings)
                return@guard
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                syncMiniCutNotifications(context, settings)
                return@guard
            }

            val message = slot.messages[now.dayOfMonth % slot.messages.size]
            val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(slot.title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            slot.requestCode,
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            NotificationManagerCompat.from(context).notify(slot.requestCode, notification)
            syncMiniCutNotifications(context, settings)
        }
    }
}

class MiniCutBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MiniCutDiagnostics.guard("MiniCutBootReceiver.onReceive") {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                syncMiniCutNotifications(context)
            }
        }
    }
}
