package com.yusufcanmercan.weight_track_app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: WeightRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** Ana sayfa durumu. */
    val uiState: StateFlow<HomeUiState> = flow {
        var previousWeights: List<Weight>? = null
        var previousZone: TimeZone? = null
        var history = HomeUiState()

        emitAll(combine(repository.observeAll(), settings.goalWeight, settings.heightCm) { weights, goal, height ->
            val zone = TimeZone.getDefault()
            if (weights !== previousWeights || zone != previousZone) {
                history = buildHomeState(weights, timeZone = zone)
                previousWeights = weights
                previousZone = zone
            }
            history.withMeasurementSettings(goal, height)
        })
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            // Ekran görünmeyince 5 sn sonra dinlemeyi bırakır; ekran döndürmede yeniden yüklemez.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}
