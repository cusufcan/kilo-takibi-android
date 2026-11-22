package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight

/** Listedeki bir satır: kayıt + bir önceki (daha eski) kayda göre fark (kg). İlk kayıtta `null`. */
data class HomeItem(val weight: Weight, val change: Double?)

/** Bir ayın satırları. [month] 0..11 (`Calendar.MONTH`); [items] en yeni üstte. */
data class MonthGroup(val year: Int, val month: Int, val items: List<HomeItem>)

/**
 * Hedefe ilerleme. [remaining] = güncel eksi hedef (kg; + = verilecek, − = alınacak, hedefe ulaşınca 0'a yakın/geçmiş).
 * [progress] 0..1; [reached] hedefe varıldı mı (kilo verme ve alma için ayrı ayrı doğru).
 */
data class GoalProgress(val goalKg: Double, val remaining: Double, val progress: Float, val reached: Boolean)

/** Ana sayfanın tek durumu. Ekran yalnız bunu çizer. */
data class HomeUiState(
    val isLoading: Boolean = true,
    /** En yeni kayıt en üstte. */
    val items: List<HomeItem> = emptyList(),
    /** Son tartı; kayıt yoksa `null`. */
    val latest: Weight? = null,
    /** Grafik ve Bugün kartı aynı eğilimi kullanır. Günler eskiden yeniye sıralıdır. */
    val dailyWeights: List<DailyWeight> = emptyList(),
    /** Geçmiş listesi için aylara gruplu satırlar; en yeni ay üstte. */
    val months: List<MonthGroup> = emptyList(),
    /** Hedef konmadıysa veya kayıt yoksa `null`. */
    val goal: GoalProgress? = null,
    /** Boy girilmediyse veya kayıt yoksa `null`. */
    val bmi: Bmi? = null,
    /** Boy girilmiş mi (kart, BMI yoksa ipucu göstersin diye). */
    val hasHeight: Boolean = false,
    val heightCm: Double? = null,
)
