package com.yusufcanmercan.weight_track_app.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Tarih yardımcıları. `timeStamp` epoch ms'dir ve "gün" kullanıcının saat dilimine göredir.
 * (java.time minSdk 25'te yok, bu yüzden Calendar.)
 */

/** Verilen anın yerel günündeki 00:00'ı. Aynı gün = aynı sayı. */
fun startOfDay(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** Material tarih seçici günü UTC gece yarısı olarak ister; yerel günü ona çevirir. */
fun toPickerMillis(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    changeZone(millis, from = zone, to = TimeZone.getTimeZone("UTC"))

/** Seçicinin verdiği UTC gece yarısını, aynı takvim gününün yerel 00:00'ına çevirir. */
fun fromPickerMillis(pickerMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    changeZone(pickerMillis, from = TimeZone.getTimeZone("UTC"), to = zone)

/** Yıl/ay/gün'ü koruyup saat dilimini değiştirir; saat 00:00 olur. */
private fun changeZone(millis: Long, from: TimeZone, to: TimeZone): Long {
    val source = Calendar.getInstance(from).apply { timeInMillis = millis }
    return Calendar.getInstance(to).apply {
        clear()
        set(source[Calendar.YEAR], source[Calendar.MONTH], source[Calendar.DAY_OF_MONTH])
    }.timeInMillis
}
