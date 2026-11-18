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
    val heightCm: Double? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(repository.themeMode, repository.goalWeight, repository.heightCm) { theme, goal, height ->
        SettingsUiState(isLoading = false, themeMode = theme, goalKg = goal, heightCm = height)
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

    /** Boyu kaydeder. Boş metin boyu siler. Geçersizse (sayı değil / 100–250 cm dışı) `false` döner. */
    fun onHeightSave(text: String): Boolean {
        if (text.isBlank()) {
            viewModelScope.launch { repository.setHeightCm(null) }
            return true
        }
        val value = text.replace(',', '.').toDoubleOrNull()
        if (value == null || value !in MIN_HEIGHT_CM..MAX_HEIGHT_CM) return false
        viewModelScope.launch { repository.setHeightCm(value) }
        return true
    }

    companion object {
        const val MIN_HEIGHT_CM = 100.0
        const val MAX_HEIGHT_CM = 250.0
    }
}
