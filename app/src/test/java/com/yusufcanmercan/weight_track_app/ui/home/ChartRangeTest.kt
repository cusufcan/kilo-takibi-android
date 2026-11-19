package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartRangeTest {

    private val day = 86_400_000L

    private fun w(kg: Double, dayNo: Long) = Weight(weight = kg, timeStamp = 1_000_000L + dayNo * day)

    // 0., 50., 92., 93., 100. günler; son kayıt 100. gün.
    private val weights = listOf(w(90.0, 0), w(85.0, 50), w(82.0, 92), w(81.5, 93), w(80.0, 100))

    @Test
    fun `tumu filtrelemez`() {
        assertEquals(weights, filterByRange(weights, ChartRange.ALL))
    }

    @Test
    fun `aralik son kayittan geriye sayilir ve sinir dahildir`() {
        // 7G: 93. gün tam sınırda (100 - 7), dahil; 92. gün dışarıda.
        assertEquals(listOf(81.5, 80.0), filterByRange(weights, ChartRange.WEEK).map { it.weight })
    }

    @Test
    fun `otuz ve doksan gun dogru kayitlari alir`() {
        // 30G: 70. günden sonrası; 90G: 10. günden sonrası (0. gün dışarıda).
        assertEquals(listOf(82.0, 81.5, 80.0), filterByRange(weights, ChartRange.MONTH).map { it.weight })
        assertEquals(listOf(85.0, 82.0, 81.5, 80.0), filterByRange(weights, ChartRange.QUARTER).map { it.weight })
    }

    @Test
    fun `bos liste bos kalir`() {
        assertTrue(filterByRange(emptyList(), ChartRange.WEEK).isEmpty())
    }
}
