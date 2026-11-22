package com.yusufcanmercan.weight_track_app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartRangeTest {

    private val day = 86_400_000L

    private fun w(kg: Double, dayNo: Long) = DailyWeight(dayMillis = dayNo * day, measuredKg = kg, trendKg = kg)

    private val weights = (0L..100L).map { w(80.0, it) }

    @Test
    fun `tumu filtrelemez`() {
        assertEquals(weights, filterByRange(weights, ChartRange.ALL))
    }

    @Test
    fun `aralik son kayittan geriye sayilir ve sinir dahildir`() {
        assertEquals(weights.takeLast(7), filterByRange(weights, ChartRange.WEEK))
    }

    @Test
    fun `otuz ve doksan gun dogru kayitlari alir`() {
        assertEquals(weights.takeLast(30), filterByRange(weights, ChartRange.MONTH))
        assertEquals(weights.takeLast(90), filterByRange(weights, ChartRange.QUARTER))
    }

    @Test
    fun `bos liste bos kalir`() {
        assertTrue(filterByRange(emptyList(), ChartRange.WEEK).isEmpty())
    }
}
