package com.yusufcanmercan.weight_track_app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Tip güvenli rotalar (ADR-0004): her ekran bir Kotlin tipi, yazım hatası derlemede yakalanır.
 * Ekle/Düzenle bir rota değil, ana sayfanın alt sayfası (ADR-0015).
 */
@Serializable
data object Home

@Serializable
data object Settings

@Serializable
data object Progress

@Serializable
data object History

@Serializable
data object Profile

@Serializable
data object Milestones

@Serializable
data class Weekly(val throughDay: Long = 0L)
