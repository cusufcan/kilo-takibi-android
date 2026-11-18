package com.yusufcanmercan.weight_track_app.ui.navigation

import kotlinx.serialization.Serializable

/** Tip güvenli rotalar (ADR-0004): her ekran bir Kotlin tipi, yazım hatası derlemede yakalanır. */
@Serializable
data object Home

@Serializable
data object Graph

@Serializable
data object Settings

/** Yalnız `id` taşınır; `null` = yeni kayıt. Argüman ViewModel'in SavedStateHandle'ına `id` adıyla düşer. */
@Serializable
data class EditWeight(val id: Long? = null)
