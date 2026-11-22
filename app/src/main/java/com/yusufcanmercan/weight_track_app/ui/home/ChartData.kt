package com.yusufcanmercan.weight_track_app.ui.home

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** İlk takvim gününden [day] gün sonra görülen tartı noktası ve eğilim çizgisi. */
data class ChartPoint(val day: Long, val measuredKg: Double, val trendKg: Double)

/**
 * Çizime hazır veri: noktalar ve eksen aralıkları. Android'e bağlı değil, yalnız hesap;
 * bu yüzden düz birim testle doğrulanır.
 */
data class ChartData(
    val points: List<ChartPoint>,
    /** İlk yerel takvim gününün UTC gece yarısı; eksen etiketi için. */
    val startMillis: Long,
    val maxDay: Long,
    val minKg: Double,
    val maxKg: Double,
    /** Son kayıt eksi ilk kayıt (kg); eksi = verilen kilo. */
    val change: Double,
    /** Hedef kilo; grafikte kesikli yatay çizgi. Yoksa `null`. */
    val goalKg: Double? = null,
    val forecast: ChartPoint? = null,
)

private const val Y_PADDING_KG = 1.0

/** Grafik alanına sığan en az üç düzenli kilo adımı. Büyük değer üstte çizilir. */
internal fun kilogramTicks(minKg: Double, maxKg: Double): List<Double> {
    val magnitude = 10.0.pow(floor(log10((maxKg - minKg) / 2)))
    val step = listOf(10.0, 5.0, 4.0, 3.0, 2.5, 2.0, 1.0, 0.5)
        .map { it * magnitude }
        .first { floor(maxKg / it) - ceil(minKg / it) >= 2 }
    return (floor(maxKg / step).toLong() downTo ceil(minKg / step).toLong()).map { it * step }
}

/**
 * Kayıtları tarih eksenli grafik verisine çevirir; kayıt yoksa `null`.
 * X ekseni kayıt sırası değil GERÇEK tarihtir: 1. ve 10. gün arasındaki boşluk grafikte de boşluktur.
 */
fun buildChartData(
    weights: List<DailyWeight>,
    goalKg: Double? = null,
    forecastDay: Long? = null,
    period: LongRange? = null,
): ChartData? {
    if (weights.isEmpty()) return null
    val sorted = weights.sortedBy { it.dayMillis }
    val first = period?.first ?: sorted.first().dayMillis
    val points = sorted.map {
        ChartPoint((it.dayMillis - first) / DAY_MILLIS, it.measuredKg, it.trendKg)
    }
    val minKg = minOf(points.minOf { minOf(it.measuredKg, it.trendKg) }, goalKg ?: Double.MAX_VALUE)
    val maxKg = maxOf(points.maxOf { maxOf(it.measuredKg, it.trendKg) }, goalKg ?: -Double.MAX_VALUE)
    val forecast = if (forecastDay != null && goalKg != null && forecastDay > sorted.last().dayMillis) {
        ChartPoint((forecastDay - first) / DAY_MILLIS, goalKg, goalKg)
    } else null
    return ChartData(
        points = points,
        startMillis = first,
        maxDay = maxOf(points.last().day, forecast?.day ?: 0, period?.let { (it.last - first) / DAY_MILLIS } ?: 0),
        // Çizgi kenarlara yapışmasın diye alt/üst 1 kg pay; tek kayıtta aralık sıfır olmaz.
        minKg = minKg - Y_PADDING_KG,
        maxKg = maxKg + Y_PADDING_KG,
        change = points.last().measuredKg - points.first().measuredKg,
        goalKg = goalKg,
        forecast = forecast,
    )
}

/**
 * Dokunulan yatay konuma (0..1, grafiğin solu..sağı) en yakın noktanın sırasını verir.
 * Yakınlık GÜN farkıyla ölçülür, çünkü X ekseni gerçek tarihtir. Tek günlük veride hep 0.
 */
fun nearestPointIndex(data: ChartData, fraction: Float): Int {
    if (data.maxDay == 0L || fraction.isNaN()) return 0
    val targetDay = fraction.coerceIn(0f, 1f) * data.maxDay
    val points = data.points
    val right = firstPointAtOrAfter(points, targetDay)
    if (right == 0) return 0
    val leftDay = points[right - 1].day.toFloat()
    if (right < points.size && points[right].day - targetDay < targetDay - leftDay) {
        return right
    }
    // Eşit uzaklıkta ve aynı gündeki noktalarda ilk noktayı koru.
    return firstPointAtOrAfter(points, leftDay)
}

private fun firstPointAtOrAfter(points: List<ChartPoint>, day: Float): Int {
    var low = 0
    var high = points.size
    while (low < high) {
        val middle = low + (high - low) / 2
        if (points[middle].day < day) low = middle + 1 else high = middle
    }
    return low
}
