package com.yusufcanmercan.weight_track_app.platform

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.yusufcanmercan.weight_track_app.MainActivity
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.util.Calendar

private const val CHANNEL_ID = "weekly_summary"
private const val ALARM_ACTION = "com.yusufcanmercan.weight_track_app.WEEKLY_ALARM"
private const val SUMMARY_DAY = "summary_day"

object WeeklyNotifications {
    private fun settings(context: Context) = context.getSharedPreferences("weekly_notification", Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = settings(context).getBoolean("enabled", false)

    fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun setEnabled(context: Context, enabled: Boolean) {
        settings(context).edit { putBoolean("enabled", enabled) }
        schedule(context)
        if (!enabled) context.getSystemService(NotificationManager::class.java).cancel(1)
    }

    internal fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val monday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, (Calendar.MONDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 7)
        }
        val sunday = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val intent = Intent(context, WeeklyNotificationReceiver::class.java).setAction(ALARM_ACTION)
            .putExtra(SUMMARY_DAY, toPickerMillis(sunday.timeInMillis))
        val pending = PendingIntent.getBroadcast(context, 10, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (isEnabled(context)) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, monday.timeInMillis, pending)
        else alarms.cancel(pending)
    }
}

class WeeklyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Saat, saat dilimi ya da yeniden başlatma günü değiştirmiş olabilir; widget da yenilensin.
        if (intent.action != ALARM_ACTION) SeyirWidget.refresh(context)
        if (!WeeklyNotifications.isEnabled(context)) return
        WeeklyNotifications.schedule(context)
        if (intent.action != ALARM_ACTION || !WeeklyNotifications.hasPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.weekly_summary), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = MainActivity.weeklyIntent(context, intent.getLongExtra(SUMMARY_DAY, 0L))
        val action = PendingIntent.getActivity(context, 11, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(
            1,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_seyir_notification)
                .setContentTitle(context.getString(R.string.weekly_summary))
                .setContentText(context.getString(R.string.weekly_notification_body))
                .setContentIntent(action)
                .setAutoCancel(true)
                .build(),
        )
    }
}
