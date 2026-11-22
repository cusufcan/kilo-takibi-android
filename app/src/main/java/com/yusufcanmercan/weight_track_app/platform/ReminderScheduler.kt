package com.yusufcanmercan.weight_track_app.platform

import android.content.Context
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Ayarlardaki bildirim tercihlerine göre alarmları kurar ya da iptal eder. */
interface ReminderScheduler {
    suspend fun sync()
}

@Singleton
class AndroidReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : ReminderScheduler {
    override suspend fun sync() {
        WeeklyNotifications.schedule(context, settings.weeklySummaryEnabled.first())
    }
}
