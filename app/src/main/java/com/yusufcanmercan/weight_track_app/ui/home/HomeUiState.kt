package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight

/** Listedeki bir satır: kayıt + bir önceki (daha eski) kayda göre fark (kg). İlk kayıtta `null`. */
data class HomeItem(val weight: Weight, val change: Double?)

/** Ana sayfanın tek durumu. Ekran yalnız bunu çizer. */
data class HomeUiState(
    val isLoading: Boolean = true,
    /** En yeni kayıt en üstte. */
    val items: List<HomeItem> = emptyList(),
    /** Güncel kilo kartı için son kayıt; kayıt yoksa `null`. */
    val latest: Weight? = null,
    /** Son kayıt eksi en az 7 gün öncesindeki kayıt (kg); karşılaştıracak kayıt yoksa `null`. */
    val weeklyChange: Double? = null,
    /** Karttaki mini grafik için eskiden yeniye son kilolar. */
    val trend: List<Double> = emptyList(),
)
