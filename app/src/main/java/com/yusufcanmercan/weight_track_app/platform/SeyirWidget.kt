package com.yusufcanmercan.weight_track_app.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import androidx.core.content.ContextCompat
import com.yusufcanmercan.weight_track_app.MainActivity
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.ui.home.calculateDailyWeights
import com.yusufcanmercan.weight_track_app.ui.home.sortedByMeasurementTime
import com.yusufcanmercan.weight_track_app.ui.home.weeklySummary
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependencies {
    fun repository(): WeightRepository
    fun settings(): SettingsRepository
}

class SeyirWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MIDNIGHT_ACTION) refresh(context) else super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(midnightIntent(context))
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        scheduleMidnight(context)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dependencies = EntryPointAccessors.fromApplication(context, WidgetDependencies::class.java)
                val weights = dependencies.repository().observeAll().first()
                val zone = TimeZone.getDefault()
                val dailyWeights = calculateDailyWeights(weights.sortedByMeasurementTime(), zone)
                val localized = ContextCompat.getContextForLanguage(context)
                val dark = when (dependencies.settings().themeMode.first()) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> localized.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                }
                val open = PendingIntent.getActivity(
                    context, 20, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val add = PendingIntent.getActivity(
                    context, 21, MainActivity.quickEntryIntent(context),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val views = RemoteViews(context.packageName, if (dark) R.layout.seyir_widget_dark else R.layout.seyir_widget)
                val trend = dailyWeights.lastOrNull()
                val weight = trend?.let { localized.getString(R.string.weight_kg, it.trendKg) } ?: "—"
                val change = weeklySummary(dailyWeights, toPickerMillis(System.currentTimeMillis(), zone)).displayChange
                val weekly = change?.let { localized.getString(if (it == 0.0) R.string.weight_kg else R.string.change_kg, it) }
                    ?: localized.getString(if (trend == null) R.string.widget_empty else R.string.weekly_summary)
                views.setTextViewText(R.id.widget_weight, SpannableString(weight).apply {
                    val unit = weight.lastIndexOf(' ')
                    if (unit >= 0) setSpan(RelativeSizeSpan(0.45f), unit, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                })
                views.setTextViewText(R.id.widget_change, weekly)
                val badgeColors = when {
                    change == null || change == 0.0 -> null
                    change < 0 -> if (dark) R.drawable.widget_badge_decrease_dark to 0xFFB7DEC9.toInt()
                    else R.drawable.widget_badge_decrease to 0xFF22604D.toInt()
                    else -> if (dark) R.drawable.widget_badge_increase_dark to 0xFFEDC7AA.toInt()
                    else R.drawable.widget_badge_increase to 0xFF78502F.toInt()
                }
                badgeColors?.let { (background, foreground) ->
                    views.setInt(R.id.widget_change, "setBackgroundResource", background)
                    views.setTextColor(R.id.widget_change, foreground)
                }
                views.setTextViewText(R.id.widget_title, localized.getString(R.string.home_trend_weight))
                views.setTextViewText(R.id.widget_add, localized.getString(R.string.widget_add))
                views.setOnClickPendingIntent(R.id.widget_root, open)
                views.setOnClickPendingIntent(R.id.widget_add, add)
                manager.updateAppWidget(appWidgetIds, views)
            } catch (error: Exception) {
                Log.w("SeyirWidget", "Widget update failed", error)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val MIDNIGHT_ACTION = "com.yusufcanmercan.weight_track_app.WIDGET_MIDNIGHT"

        private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 22, Intent(context, SeyirWidget::class.java).setAction(MIDNIGHT_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /**
         * Gün değişince "bu hafta" ve eğilim yeniden hesaplansın diye gece yarısından hemen sonrasına
         * uyandırmayan bir alarm kurar. Telefon uykudaysa alarm ekran ilk açıldığında çalışır; kullanıcı
         * bir şey yapmaz. Her güncellemede yeniden kurulur, yeniden başlatmada sistem widget'ı zaten günceller.
         */
        private fun scheduleMidnight(context: Context) {
            val nextDay = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            context.getSystemService(AlarmManager::class.java)
                .set(AlarmManager.RTC, nextDay.timeInMillis, midnightIntent(context))
        }

        fun canPin(context: Context): Boolean = Build.VERSION.SDK_INT >= 26 &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

        fun pin(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                AppWidgetManager.getInstance(context).requestPinAppWidget(
                    ComponentName(context, SeyirWidget::class.java), null, null,
                )
            }
        }

        fun refresh(context: Context) {
            val component = ComponentName(context, SeyirWidget::class.java)
            val widgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(component)
            if (widgetIds.isEmpty()) return
            context.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(component)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds))
        }
    }
}
