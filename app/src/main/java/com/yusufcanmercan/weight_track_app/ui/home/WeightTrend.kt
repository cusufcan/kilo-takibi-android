package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.util.TimeZone
import kotlin.math.pow

internal const val DAY_MILLIS = 86_400_000L
private const val MEASUREMENT_WEIGHT = 0.25

/** Yerel takvim günü, grafik ve karşılaştırmalar için UTC gece yarısı olarak temsil edilir. */
data class DailyWeight(val dayMillis: Long, val measuredKg: Double, val trendKg: Double)

/**
 * Her günün son ölçümüyle eğilimi hesaplar. Ölçümün ağırlığı aradaki gün sayısıyla büyür;
 * günlük tartılan için [MEASUREMENT_WEIGHT] kalır.
 */
internal fun calculateDailyWeights(oldestFirst: List<Weight>, timeZone: TimeZone): List<DailyWeight> {
    val byDay = oldestFirst.associateBy { toPickerMillis(it.timeStamp, timeZone) }
    var previous: DailyWeight? = null
    return byDay.map { (day, weight) ->
        val measured = weight.weight
        val trend = previous?.let { measuredWeight(day - it.dayMillis).let { a -> it.trendKg + (measured - it.trendKg) * a } }
            ?: measured
        DailyWeight(day, measured, trend).also { previous = it }
    }
}

/** Aradaki gün sayısına göre yeni ölçümün ağırlığı: 1 − (1 − w)^gün. */
internal fun measuredWeight(gapMillis: Long): Double {
    val gapDays = maxOf(1L, Math.round(gapMillis.toDouble() / DAY_MILLIS))
    return 1 - (1 - MEASUREMENT_WEIGHT).pow(gapDays.toDouble())
}
