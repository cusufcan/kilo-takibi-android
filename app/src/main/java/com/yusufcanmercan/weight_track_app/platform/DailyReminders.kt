package com.yusufcanmercan.weight_track_app.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yusufcanmercan.weight_track_app.MainActivity
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.DailyReminder
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.ui.home.DailyWeight
import com.yusufcanmercan.weight_track_app.ui.home.calculateDailyWeights
import com.yusufcanmercan.weight_track_app.ui.home.currentStreak
import com.yusufcanmercan.weight_track_app.ui.home.sortedByMeasurementTime
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "daily_reminder"
private const val ALARM_ACTION = "com.yusufcanmercan.weight_track_app.DAILY_REMINDER"
private const val NOTIFICATION_ID = 2
private const val MIN_STREAK_FOR_MESSAGE = 2

/** Bildirimde ne yazacağı; bugün tartılmışsa bildirim hiç gösterilmez. */
internal sealed interface ReminderMessage {
    data object Skip : ReminderMessage
    data object Plain : ReminderMessage
    data class Streak(val days: Int) : ReminderMessage
}

internal fun reminderMessage(dailyWeights: List<DailyWeight>, today: Long): ReminderMessage {
    if (dailyWeights.lastOrNull()?.dayMillis == today) return ReminderMessage.Skip
    val streak = currentStreak(dailyWeights, today)
    return if (streak >= MIN_STREAK_FOR_MESSAGE) ReminderMessage.Streak(streak) else ReminderMessage.Plain
}

/** Seçilen saatin bir sonraki yerel karşılığı; saat geçtiyse yarın. */
internal fun nextReminderAt(now: Long, minuteOfDay: Int, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

object DailyReminders {
    internal fun schedule(context: Context, reminder: DailyReminder) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DailyReminderReceiver::class.java).setAction(ALARM_ACTION)
        val pending = PendingIntent.getBroadcast(context, 12, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (reminder.enabled) {
            // Kesin alarm izni istemiyoruz; Android birkaç dakika kaydırabilir (ADR-0040).
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextReminderAt(System.currentTimeMillis(), reminder.minuteOfDay), pending)
        } else {
            alarms.cancel(pending)
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }
    }
}

// Uzun biçim: Kotlin soyut `super.onReceive` çağrısına izin vermez, enjeksiyon Hilt_ tabanında olur.
@AndroidEntryPoint(BroadcastReceiver::class)
class DailyReminderReceiver : Hilt_DailyReminderReceiver() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var weights: WeightRepository

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ALARM_ACTION) return
        val result = goAsync()
        receiverScope.launch {
            try {
                val reminder = settings.dailyReminder.first()
                DailyReminders.schedule(context, reminder)
                if (!reminder.enabled || !WeeklyNotifications.hasPermission(context)) return@launch
                val zone = TimeZone.getDefault()
                val daily = calculateDailyWeights(weights.observeAll().first().sortedByMeasurementTime(), zone)
                show(context, reminderMessage(daily, toPickerMillis(System.currentTimeMillis(), zone)))
            } finally {
                result.finish()
            }
        }
    }

    private fun show(context: Context, message: ReminderMessage) {
        val body = when (message) {
            ReminderMessage.Skip -> return
            ReminderMessage.Plain -> context.getString(R.string.reminder_body)
            is ReminderMessage.Streak -> context.resources.getQuantityString(R.plurals.reminder_body_streak, message.days, message.days)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            context, 13, MainActivity.quickEntryIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_seyir_notification)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(body)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}
