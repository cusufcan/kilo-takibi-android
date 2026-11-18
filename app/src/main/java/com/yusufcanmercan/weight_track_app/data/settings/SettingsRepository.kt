package com.yusufcanmercan.weight_track_app.data.settings

import kotlinx.coroutines.flow.Flow

/** Kullanıcı ayarlarının tek giriş kapısı (ADR-0008). Dil burada değil: onu Android saklar. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
