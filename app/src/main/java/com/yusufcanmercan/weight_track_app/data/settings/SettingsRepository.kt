package com.yusufcanmercan.weight_track_app.data.settings

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlinx.coroutines.flow.Flow

/** Günlük tartı hatırlatıcısı; saat gün içindeki dakika olarak tutulur. */
data class DailyReminder(val enabled: Boolean = false, val minuteOfDay: Int = DEFAULT_MINUTE) {
    companion object {
        const val DEFAULT_MINUTE = 8 * 60
    }
}

data class OnboardingState(val completed: Boolean = false, val pendingWeight: Weight? = null)

/** Kullanıcı ayarları. Dili Android saklar. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Hedef kilo (kg); `null` = hedef yok. */
    val goalWeight: Flow<Double?>

    suspend fun setGoalWeight(kg: Double?)

    /** Boy (cm); `null` = girilmemiş. BMI için. */
    val heightCm: Flow<Double?>

    suspend fun setHeightCm(cm: Double?)

    val displayName: Flow<String?>

    suspend fun setDisplayName(name: String?)

    val onboarding: Flow<OnboardingState>

    /** İlk gönderimi saklar ve Room'a yazılması beklenen tartıyı döndürür. */
    suspend fun saveOnboarding(name: String?, heightCm: Double?, goalKg: Double?, firstWeight: Weight?): Weight?

    suspend fun completeOnboarding()

    /** Pazartesi akşamı haftalık özet bildirimi; varsayılan kapalı. */
    val weeklySummaryEnabled: Flow<Boolean>

    suspend fun setWeeklySummaryEnabled(enabled: Boolean)

    /** Varsayılan kapalı, 08:00; ilk açılışta sorulmaz. */
    val dailyReminder: Flow<DailyReminder>

    suspend fun setDailyReminder(reminder: DailyReminder)
}
