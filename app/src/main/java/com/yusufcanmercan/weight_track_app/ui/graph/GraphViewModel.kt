package com.yusufcanmercan.weight_track_app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** [data] `null` ise henüz kayıt yok. */
data class GraphUiState(
    val isLoading: Boolean = true,
    val data: ChartData? = null,
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    repository: WeightRepository,
) : ViewModel() {

    val uiState: StateFlow<GraphUiState> = repository.observeAll()
        .map { GraphUiState(isLoading = false, data = buildChartData(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphUiState())
}
