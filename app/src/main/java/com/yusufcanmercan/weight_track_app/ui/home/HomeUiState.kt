package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight

/** Ana sayfanın tek durumu. Ekran yalnız bunu çizer. */
data class HomeUiState(
    val isLoading: Boolean = true,
    /** En yeni kayıt en üstte. */
    val items: List<Weight> = emptyList(),
)
