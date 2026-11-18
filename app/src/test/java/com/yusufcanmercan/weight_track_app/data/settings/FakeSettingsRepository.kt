package com.yusufcanmercan.weight_track_app.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Testte gerçek DataStore yerine kullanılan elle yazılmış sahte. */
class FakeSettingsRepository(initial: ThemeMode = ThemeMode.SYSTEM, initialGoal: Double? = null) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val themeMode: Flow<ThemeMode> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }

    private val goal = MutableStateFlow(initialGoal)
    override val goalWeight: Flow<Double?> = goal

    override suspend fun setGoalWeight(kg: Double?) {
        goal.value = kg
    }
}
