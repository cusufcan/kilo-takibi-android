package com.yusufcanmercan.weight_track_app.ui.edit

/** Ekle/Düzenle ekranının tek durumu. */
data class EditUiState(
    /** Kullanıcının yazdığı metin; sayıya çevirmeyi kaydederken yaparız. */
    val weightText: String = "",
    /** Not metni; boş bırakılabilir. */
    val noteText: String = "",
    val dateMillis: Long = 0L,
    val hasMeasurementTime: Boolean = false,
    val mood: EntryMood? = null,
    val isEditing: Boolean = false,
    val weightError: Boolean = false,
    /** Yazma işlemi tamamlandı. Yeni kayıtta sonuç gösterilir. */
    val saved: Boolean = false,
    val savedEntryId: Long? = null,
    val busy: Boolean = false,
    val writeError: Boolean = false,
    val available: Boolean = true,
)

enum class EntryMood { GOOD, CALM, TIRED }
