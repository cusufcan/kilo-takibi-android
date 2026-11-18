package com.yusufcanmercan.weight_track_app.ui.edit

/** Ekle/Düzenle ekranının tek durumu. */
data class EditUiState(
    /** Kullanıcının yazdığı metin; sayıya çevirmeyi kaydederken yaparız. */
    val weightText: String = "",
    /** Seçili günün yerel 00:00'ı. */
    /** Not metni; boş bırakılabilir. */
    val noteText: String = "",
    val dateMillis: Long = 0L,
    val isEditing: Boolean = false,
    val weightError: Boolean = false,
    /** Kaydedildi; ekran bunu görünce kapanır. */
    val saved: Boolean = false,
)
