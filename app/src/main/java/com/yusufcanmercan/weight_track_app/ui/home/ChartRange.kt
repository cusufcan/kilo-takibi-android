package com.yusufcanmercan.weight_track_app.ui.home

/** Grafik aralığı; [days] `null` ise tüm kayıtlar. */
enum class ChartRange(val days: Int?) { WEEK(7), MONTH(30), QUARTER(90), ALL(null) }

/**
 * Seçili aralığa düşen kayıtlar. Aralık bugünden değil SON KAYITTAN geriye sayılır:
 * bir süre kayıt girilmese de son haftanın verisini gösterir. Son kayıt günü aralığın içindedir.
 */
fun filterByRange(weights: List<DailyWeight>, range: ChartRange): List<DailyWeight> {
    val days = range.days ?: return weights
    val latest = weights.maxOfOrNull { it.dayMillis } ?: return weights
    val from = latest - (days - 1) * DAY_MILLIS
    return weights.filter { it.dayMillis >= from }
}
