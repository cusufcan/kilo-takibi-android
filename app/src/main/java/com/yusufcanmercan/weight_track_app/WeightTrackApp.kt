package com.yusufcanmercan.weight_track_app

import android.app.Application
import com.yusufcanmercan.weight_track_app.platform.ReminderScheduler
import com.yusufcanmercan.weight_track_app.platform.receiverScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Hilt'in bağımlılık grafiğini uygulama açılırken kurmasını sağlayan giriş noktası. */
@HiltAndroidApp
class WeightTrackApp : Application() {
    @Inject lateinit var reminders: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // Yedekten geri yüklenen "bildirim açık" tercihlerinin alarmı yoktur; açılışta kurulur.
        receiverScope.launch { reminders.sync() }
    }
}
