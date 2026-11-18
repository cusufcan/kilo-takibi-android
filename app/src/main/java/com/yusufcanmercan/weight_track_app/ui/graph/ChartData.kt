package com.yusufcanmercan.weight_track_app.ui.graph

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlin.math.roundToLong

/** Grafikte bir nokta: [day] = ilk kayıttan beri geçen gün sayısı, [kg] = kilo. */
data class ChartPoint(val day: Long, val kg: Double)

/**
 * Çizime hazır veri: noktalar ve eksen aralıkları. Android'e bağlı değil, yalnız hesap;
 * bu yüzden düz birim testle doğrulanır.
 */
data class ChartData(
    val points: List<ChartPoint>,
    /** İlk kaydın tarihi (epoch ms); eksen etiketi için. */
    val startMillis: Long,
    val maxDay: Long,
    val minKg: Double,
    val maxKg: Double,
    /** Son kayıt eksi ilk kayıt (kg); eksi = verilen kilo. */
    val change: Double,
)

const val DAY_MS = 86_400_000.0
private const val Y_PADDING_KG = 1.0

/**
 * Kayıtları tarih eksenli grafik verisine çevirir; kayıt yoksa `null`.
 * X ekseni kayıt sırası değil GERÇEK tarihtir: 1. ve 10. gün arasındaki boşluk grafikte de boşluktur.
 */
fun buildChartData(weights: List<Weight>): ChartData? {
    if (weights.isEmpty()) return null
    val sorted = weights.sortedBy { it.timeStamp }
    val first = sorted.first().timeStamp
    // Yaz saati geçişinde bir gün 23/25 saat olur; bölüp yuvarlamak bunu tolere eder.
    val points = sorted.map { ChartPoint(((it.timeStamp - first) / DAY_MS).roundToLong(), it.weight) }
    val minKg = points.minOf { it.kg }
    val maxKg = points.maxOf { it.kg }
    return ChartData(
        points = points,
        startMillis = first,
        maxDay = points.last().day,
        // Çizgi kenarlara yapışmasın diye alt/üst 1 kg pay; tek kayıtta aralık sıfır olmaz.
        minKg = minKg - Y_PADDING_KG,
        maxKg = maxKg + Y_PADDING_KG,
        change = points.last().kg - points.first().kg,
    )
}

/**
 * Dokunulan yatay konuma (0..1, grafiğin solu..sağı) en yakın noktanın sırasını verir.
 * Yakınlık GÜN farkıyla ölçülür, çünkü X ekseni gerçek tarihtir. Tek günlük veride hep 0.
 */
fun nearestPointIndex(data: ChartData, fraction: Float): Int {
    if (data.maxDay == 0L) return 0
    val targetDay = fraction.coerceIn(0f, 1f) * data.maxDay
    return data.points.indices.minBy { kotlin.math.abs(data.points[it].day - targetDay) }
}
