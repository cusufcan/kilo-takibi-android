package com.yusufcanmercan.weight_track_app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ayarları DataStore'da (anahtar-değer deposu) tutar. */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        // Kayıt yoksa ya da bozuksa çökmek yerine SYSTEM'e dön.
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_KEY] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
    }
}
