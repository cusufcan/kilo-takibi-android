package com.yusufcanmercan.weight_track_app.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Testte gerçek DataStore yerine kullanılan elle yazılmış sahte. */
class FakeSettingsRepository(initial: ThemeMode = ThemeMode.SYSTEM) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val themeMode: Flow<ThemeMode> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}
