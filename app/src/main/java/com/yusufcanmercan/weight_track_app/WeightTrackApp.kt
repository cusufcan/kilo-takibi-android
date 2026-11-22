package com.yusufcanmercan.weight_track_app

import android.app.Application
import com.yusufcanmercan.weight_track_app.platform.WeeklyNotifications
import dagger.hilt.android.HiltAndroidApp

/** Hilt'in bağımlılık grafiğini uygulama açılırken kurmasını sağlayan giriş noktası. */
@HiltAndroidApp
class WeightTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Yedekten geri yüklenen "haftalık bildirim açık" tercihinin alarmı yoktur; açılışta kurulur.
        WeeklyNotifications.schedule(this)
    }
}
