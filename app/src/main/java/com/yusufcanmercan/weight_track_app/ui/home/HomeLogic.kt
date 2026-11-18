package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight

private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
private const val TREND_SIZE = 10

/**
 * Kayıtlardan ana sayfa durumunu üretir. Android'e bağlı değil, yalnız hesap; düz birim testle doğrulanır.
 * Sıra repository'den gelene güvenmez, tarihe göre kendisi sıralar.
 */
fun buildHomeState(weights: List<Weight>): HomeUiState {
    val oldestFirst = weights.sortedBy { it.timeStamp }
    val latest = oldestFirst.lastOrNull()
    val items = oldestFirst.mapIndexed { index, weight ->
        HomeItem(weight, change = oldestFirst.getOrNull(index - 1)?.let { weight.weight - it.weight })
    }.asReversed()
    // Geçen hafta karşılaştırması: son kayıttan en az 7 gün önceki en yeni kayıt.
    val weekAgo = latest?.let { l -> oldestFirst.lastOrNull { it.timeStamp <= l.timeStamp - WEEK_MS } }
    return HomeUiState(
        isLoading = false,
        items = items,
        latest = latest,
        weeklyChange = if (latest != null && weekAgo != null) latest.weight - weekAgo.weight else null,
        trend = oldestFirst.takeLast(TREND_SIZE).map { it.weight },
    )
}
