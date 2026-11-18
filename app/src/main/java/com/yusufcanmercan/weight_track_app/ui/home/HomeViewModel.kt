package com.yusufcanmercan.weight_track_app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeightRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** Durum aşağı akar: Repository -> ViewModel -> ekran (ADR-0001). */
    val uiState: StateFlow<HomeUiState> = combine(repository.observeAll(), settings.goalWeight, ::buildHomeState)
        .stateIn(
            scope = viewModelScope,
            // Ekran görünmeyince 5 sn sonra dinlemeyi bırakır; ekran döndürmede yeniden yüklemez.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /** Olay yukarı akar: ekran -> ViewModel. */
    fun onDelete(weight: Weight) {
        viewModelScope.launch { repository.delete(weight) }
    }
}
