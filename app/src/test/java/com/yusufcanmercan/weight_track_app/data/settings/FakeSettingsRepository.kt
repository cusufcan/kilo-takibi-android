package com.yusufcanmercan.weight_track_app.data.settings

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Testte gerçek DataStore yerine kullanılan elle yazılmış sahte. */
class FakeSettingsRepository(initial: ThemeMode = ThemeMode.SYSTEM, initialGoal: Double? = null, initialHeight: Double? = null) : SettingsRepository {
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

    private val height = MutableStateFlow(initialHeight)
    override val heightCm: Flow<Double?> = height

    override suspend fun setHeightCm(cm: Double?) {
        height.value = cm
    }

    private val name = MutableStateFlow<String?>(null)
    override val displayName: Flow<String?> = name

    override suspend fun setDisplayName(name: String?) {
        this.name.value = name
    }

    private val onboardingState = MutableStateFlow(OnboardingState())
    override val onboarding: Flow<OnboardingState> = onboardingState

    override suspend fun saveOnboarding(name: String?, heightCm: Double?, goalKg: Double?, firstWeight: Weight?): Weight? {
        val state = onboardingState.value
        if (state.completed || state.pendingWeight != null) return state.pendingWeight
        if (name != null) setDisplayName(name)
        if (heightCm != null) setHeightCm(heightCm)
        if (goalKg != null) setGoalWeight(goalKg)
        onboardingState.value = OnboardingState(completed = firstWeight == null, pendingWeight = firstWeight)
        return firstWeight
    }

    override suspend fun completeOnboarding() {
        onboardingState.value = OnboardingState(completed = true)
    }

    private val weekly = MutableStateFlow(false)
    override val weeklySummaryEnabled: Flow<Boolean> = weekly

    override suspend fun setWeeklySummaryEnabled(enabled: Boolean) {
        weekly.value = enabled
    }
}
