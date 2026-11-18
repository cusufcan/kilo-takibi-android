package com.yusufcanmercan.weight_track_app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** [data] `null` ise henüz kayıt yok. */
data class GraphUiState(
    val isLoading: Boolean = true,
    val data: ChartData? = null,
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    repository: WeightRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<GraphUiState> = combine(repository.observeAll(), settings.goalWeight) { weights, goal ->
        GraphUiState(isLoading = false, data = buildChartData(weights, goal))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphUiState())
}
