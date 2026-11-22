package com.yusufcanmercan.weight_track_app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yusufcanmercan.weight_track_app.data.local.Weight
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Ayarları DataStore'da (anahtar-değer deposu) tutar. */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    /**
     * Dosya okunamazsa (disk hatası, bozuk dosya) uygulama çökmez; ayarlar varsayılanlarıyla gelir.
     * Programlama hataları yine fırlatılır.
     */
    private val prefs: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val themeMode: Flow<ThemeMode> = prefs.map { prefs ->
        // Kayıt yoksa ya da bozuksa çökmek yerine SYSTEM'e dön.
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_KEY] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    override val goalWeight: Flow<Double?> = prefs.map { it[GOAL_KEY] }

    override suspend fun setGoalWeight(kg: Double?) {
        dataStore.edit { if (kg == null) it.remove(GOAL_KEY) else it[GOAL_KEY] = kg }
    }

    override val heightCm: Flow<Double?> = prefs.map { it[HEIGHT_KEY] }

    override suspend fun setHeightCm(cm: Double?) {
        dataStore.edit { if (cm == null) it.remove(HEIGHT_KEY) else it[HEIGHT_KEY] = cm }
    }

    override val displayName: Flow<String?> = prefs.map { it[DISPLAY_NAME_KEY] }

    override suspend fun setDisplayName(name: String?) {
        dataStore.edit { if (name == null) it.remove(DISPLAY_NAME_KEY) else it[DISPLAY_NAME_KEY] = name }
    }

    override val onboarding: Flow<OnboardingState> = prefs.map { prefs ->
        OnboardingState(
            completed = prefs[ONBOARDING_COMPLETED] == true,
            pendingWeight = prefs.pendingWeight(),
        )
    }

    override suspend fun saveOnboarding(
        name: String?,
        heightCm: Double?,
        goalKg: Double?,
        firstWeight: Weight?,
    ): Weight? {
        val stored = dataStore.edit { prefs ->
            if (prefs[ONBOARDING_COMPLETED] == true || prefs.pendingWeight() != null) return@edit
            if (name != null) prefs[DISPLAY_NAME_KEY] = name
            if (heightCm != null) prefs[HEIGHT_KEY] = heightCm
            if (goalKg != null) prefs[GOAL_KEY] = goalKg
            if (firstWeight == null) {
                prefs[ONBOARDING_COMPLETED] = true
            } else {
                prefs[PENDING_WEIGHT_KG] = firstWeight.weight
                prefs[PENDING_WEIGHT_TIME] = firstWeight.timeStamp
            }
        }
        return stored.pendingWeight()
    }

    private fun Preferences.pendingWeight(): Weight? {
        val kg = this[PENDING_WEIGHT_KG] ?: return null
        val time = this[PENDING_WEIGHT_TIME] ?: return null
        return Weight(weight = kg, timeStamp = time, hasMeasurementTime = true)
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = true
            prefs.remove(PENDING_WEIGHT_KG)
            prefs.remove(PENDING_WEIGHT_TIME)
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val GOAL_KEY = doublePreferencesKey("goal_weight")
        val HEIGHT_KEY = doublePreferencesKey("height_cm")
        val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PENDING_WEIGHT_KG = doublePreferencesKey("onboarding_weight_kg")
        val PENDING_WEIGHT_TIME = longPreferencesKey("onboarding_weight_time")
    }
}
