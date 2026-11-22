package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import java.util.Calendar
import java.util.TimeZone

internal fun List<Weight>.sortedByMeasurementTime(): List<Weight> =
    sortedWith(compareBy(Weight::timeStamp, Weight::id))

/**
 * Satırları yerel takvim ayına göre gruplar; gelen sıra korunur (en yeni üstte gelir, en yeni ay üstte kalır).
 * `java.time` minSdk 25'te yok, bu yüzden `Calendar`. Saat dilimi test için dışarıdan verilebilir.
 */
fun groupByMonth(items: List<HomeItem>, timeZone: TimeZone = TimeZone.getDefault()): List<MonthGroup> {
    val calendar = Calendar.getInstance(timeZone)
    return items
        .groupBy { item ->
            calendar.timeInMillis = item.weight.timeStamp
            calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH)
        }
        .map { (key, monthItems) -> MonthGroup(key.first, key.second, monthItems) }
}

/**
 * Kayıtlardan ana sayfa durumunu üretir. Android'e bağlı değil, yalnız hesap; düz birim testle doğrulanır.
 * Sıra repository'den gelene güvenmez, tarihe göre kendisi sıralar.
 */
fun buildHomeState(
    weights: List<Weight>,
    goalKg: Double? = null,
    heightCm: Double? = null,
    timeZone: TimeZone = TimeZone.getDefault(),
): HomeUiState {
    val oldestFirst = weights.sortedByMeasurementTime()
    val latest = oldestFirst.lastOrNull()
    val items = oldestFirst.mapIndexed { index, weight ->
        HomeItem(weight, change = oldestFirst.getOrNull(index - 1)?.let { weight.weight - it.weight })
    }.asReversed()
    val dailyWeights = calculateDailyWeights(oldestFirst, timeZone)
    return HomeUiState(
        isLoading = false,
        items = items,
        latest = latest,
        dailyWeights = dailyWeights,
        months = groupByMonth(items, timeZone),
    ).withMeasurementSettings(goalKg, heightCm)
}

internal fun HomeUiState.withMeasurementSettings(goalKg: Double?, heightCm: Double?): HomeUiState {
    val startingWeight = items.lastOrNull()?.weight?.weight
    val current = dailyWeights.lastOrNull()
    val latestWeight = latest?.weight
    return copy(
        bmi = if (heightCm != null && latestWeight != null) calculateBmi(latestWeight, heightCm) else null,
        hasHeight = heightCm != null,
        heightCm = heightCm,
        goal = if (goalKg != null && startingWeight != null && current != null) {
            goalProgress(startingWeight, current.trendKg, goalKg)
        } else null,
    )
}

/**
 * Başlangıç (ilk kayıt) → hedef yolunda şu an neredeyiz. Yön başlangıçtan çıkar:
 * başlangıç hedeften büyükse kilo verme, küçükse alma. Başlangıç = hedef ise yalnız tam eşitlik "ulaştı" sayılır.
 */
fun goalProgress(startKg: Double, currentKg: Double, goalKg: Double): GoalProgress {
    val reached = when {
        startKg > goalKg -> currentKg <= goalKg
        startKg < goalKg -> currentKg >= goalKg
        else -> currentKg == goalKg
    }
    val progress = when {
        reached -> 1f
        startKg == goalKg -> 0f
        else -> ((startKg - currentKg) / (startKg - goalKg)).toFloat().coerceIn(0f, 1f)
    }
    return GoalProgress(goalKg, remaining = currentKg - goalKg, progress = progress, reached = reached)
}
