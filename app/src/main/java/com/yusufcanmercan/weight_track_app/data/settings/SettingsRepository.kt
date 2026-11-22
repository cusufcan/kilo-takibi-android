package com.yusufcanmercan.weight_track_app.data.settings

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlinx.coroutines.flow.Flow

data class OnboardingState(val completed: Boolean = false, val pendingWeight: Weight? = null)

/** Kullanıcı ayarlarının tek giriş kapısı (ADR-0008). Dil burada değil: onu Android saklar. */
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
}
