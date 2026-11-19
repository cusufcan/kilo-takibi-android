package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight

/** Grafik aralığı; [days] `null` ise tüm kayıtlar. */
enum class ChartRange(val days: Int?) { WEEK(7), MONTH(30), QUARTER(90), ALL(null) }

/**
 * Seçili aralığa düşen kayıtlar. Aralık bugünden değil SON KAYITTAN geriye sayılır:
 * bir süre kayıt girilmese de "7G" boş kalmaz, son haftanın verisini gösterir. Sınır dahildir.
 */
fun filterByRange(weights: List<Weight>, range: ChartRange): List<Weight> {
    val days = range.days ?: return weights
    val latest = weights.maxOfOrNull { it.timeStamp } ?: return weights
    val from = latest - days * DAY_MS.toLong()
    return weights.filter { it.timeStamp >= from }
}
