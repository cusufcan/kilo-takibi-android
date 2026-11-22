package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.yusufcanmercan.weight_track_app.util.startOfDay
import java.util.TimeZone

/**
 * Bir kilo kaydı. Tablo ve sütun adları 2.3.0 ile aynı kalmalı (`timeStamp` dahil);
 * değişirse mevcut kullanıcıların verisi açılamaz.
 */
@Entity(tableName = "Weight")
data class Weight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weight: Double,
    /** Kaydın tarihi, epoch milisaniye (1 Ocak 1970'ten beri geçen ms). */
    val timeStamp: Long,
    /** İsteğe bağlı not (v2'de eklendi); eski kayıtlarda ve notsuz kayıtlarda `null`. */
    val note: String? = null,
    val mood: String? = null,
    @ColumnInfo(defaultValue = "0") val hasMeasurementTime: Boolean = false,
)

/** Eski kayıtlarda saat alanı yoktu. Gece yarısı dışındaki zaman damgaları saat içerir. */
internal fun Weight.hasKnownMeasurementTime(zone: TimeZone = TimeZone.getDefault()): Boolean =
    hasMeasurementTime || timeStamp != startOfDay(timeStamp, zone)
