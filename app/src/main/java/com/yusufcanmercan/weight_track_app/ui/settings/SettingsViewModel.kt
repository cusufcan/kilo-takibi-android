package com.yusufcanmercan.weight_track_app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val goalKg: Double? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(repository.themeMode, repository.goalWeight) { theme, goal ->
        SettingsUiState(isLoading = false, themeMode = theme, goalKg = goal)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onThemeChange(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    /**
     * Hedefi kaydeder. Boş metin hedefi siler. Geçersizse (sayı değil / 1–500 dışı) kaydetmez, `false` döner.
     */
    fun onGoalSave(text: String): Boolean {
        if (text.isBlank()) {
            viewModelScope.launch { repository.setGoalWeight(null) }
            return true
        }
        // Virgüllü giriş ("72,5") da kabul.
        val value = text.replace(',', '.').toDoubleOrNull()
        if (value == null || value !in EditWeightViewModel.MIN_KG..EditWeightViewModel.MAX_KG) return false
        viewModelScope.launch { repository.setGoalWeight(value) }
        return true
    }
}
