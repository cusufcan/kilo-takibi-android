package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.util.TimeZone

internal const val DAY_MILLIS = 86_400_000L
private const val MEASUREMENT_WEIGHT = 0.25

/** Yerel takvim günü, grafik ve karşılaştırmalar için UTC gece yarısı olarak temsil edilir. */
data class DailyWeight(val dayMillis: Long, val measuredKg: Double, val trendKg: Double)

/** Eskiden yeniye sıralı kayıtlardan her günün son ölçümüyle eğilimi hesaplar. */
internal fun calculateDailyWeights(oldestFirst: List<Weight>, timeZone: TimeZone): List<DailyWeight> {
    val byDay = oldestFirst.associateBy { toPickerMillis(it.timeStamp, timeZone) }
    var previousTrend: Double? = null
    return byDay.map { (day, weight) ->
        val measured = weight.weight
        val trend = previousTrend?.let { it + (measured - it) * MEASUREMENT_WEIGHT } ?: measured
        previousTrend = trend
        DailyWeight(day, measured, trend)
    }
}
