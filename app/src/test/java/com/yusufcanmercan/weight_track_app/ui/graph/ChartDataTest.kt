package com.yusufcanmercan.weight_track_app.ui.graph

import com.yusufcanmercan.weight_track_app.data.local.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartDataTest {

    private val day = 86_400_000L

    private fun w(kg: Double, dayNo: Long) = Weight(weight = kg, timeStamp = 1_000_000L + dayNo * day)

    @Test
    fun `kayit yoksa null doner`() {
        assertNull(buildChartData(emptyList()))
    }

    @Test
    fun `x ekseni siraya degil gercek tarihe gore hesaplanir`() {
        val data = buildChartData(listOf(w(80.0, 0), w(79.0, 1), w(78.0, 10)))!!

        assertEquals(listOf(0L, 1L, 10L), data.points.map { it.day })
        assertEquals(10L, data.maxDay)
    }

    @Test
    fun `girdi sirasi karisik olsa da tarihe gore siralanir`() {
        val data = buildChartData(listOf(w(78.0, 10), w(80.0, 0)))!!

        assertEquals(listOf(80.0, 78.0), data.points.map { it.kg })
    }

    @Test
    fun `y ekseni en dusuk ve en yuksek kiloya birer kilo pay ekler`() {
        val data = buildChartData(listOf(w(80.0, 0), w(78.0, 5), w(81.5, 9)))!!

        assertEquals(77.0, data.minKg, 0.0001)
        assertEquals(82.5, data.maxKg, 0.0001)
    }

    @Test
    fun `degisim son eksi ilk kilodur`() {
        val data = buildChartData(listOf(w(80.0, 0), w(83.0, 3), w(78.5, 9)))!!

        assertEquals(-1.5, data.change, 0.0001)
    }

    @Test
    fun `tek kayitta aralik sifir olmaz ve degisim sifirdir`() {
        val data = buildChartData(listOf(w(80.0, 0)))!!

        assertEquals(0L, data.maxDay)
        assertEquals(2.0, data.maxKg - data.minKg, 0.0001)
        assertEquals(0.0, data.change, 0.0001)
    }

    @Test
    fun `hedef eksen araligina dahil edilir`() {
        val d = buildChartData(listOf(w(80.0, 0), w(78.0, 1)), goalKg = 70.0)!!

        assertEquals(70.0, d.goalKg!!, 1e-9)
        assertEquals(69.0, d.minKg, 1e-9)
    }

    @Test
    fun `hedef yoksa aralik yalniz kayitlardan gelir`() {
        val d = buildChartData(listOf(w(80.0, 0), w(78.0, 1)))!!

        assertNull(d.goalKg)
        assertEquals(77.0, d.minKg, 1e-9)
    }
}

class NearestPointTest {
    private val day = 24 * 60 * 60 * 1000L
    private fun data(vararg days: Int) = buildChartData(days.map { Weight(it.toLong(), 80.0, it * day) })!!

    @org.junit.Test
    fun `dokunulan yere gun olarak en yakin nokta secilir`() {
        val d = data(0, 2, 10)
        org.junit.Assert.assertEquals(0, nearestPointIndex(d, 0.05f))
        org.junit.Assert.assertEquals(1, nearestPointIndex(d, 0.3f))
        org.junit.Assert.assertEquals(2, nearestPointIndex(d, 0.9f))
    }

    @org.junit.Test
    fun `sinir disi konum kenara sabitlenir`() {
        val d = data(0, 5)
        org.junit.Assert.assertEquals(0, nearestPointIndex(d, -1f))
        org.junit.Assert.assertEquals(1, nearestPointIndex(d, 2f))
    }

    @org.junit.Test
    fun `tek kayitta sifir doner`() {
        org.junit.Assert.assertEquals(0, nearestPointIndex(data(3), 0.7f))
    }
}
