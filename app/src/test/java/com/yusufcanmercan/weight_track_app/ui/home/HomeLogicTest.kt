package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HomeLogicTest {
    private val day = 24 * 60 * 60 * 1000L
    private fun w(id: Long, kg: Double, dayNo: Int) = Weight(id, kg, dayNo * day)

    @Test
    fun `kayit yoksa durum bostur ama yukleme bitmistir`() {
        val state = buildHomeState(emptyList())

        assertFalse(state.isLoading)
        assertNull(state.latest)
        assertNull(state.weeklyChange)
        assertEquals(emptyList<HomeItem>(), state.items)
    }

    @Test
    fun `her satirin farki bir onceki eski kayda gore hesaplanir`() {
        val state = buildHomeState(listOf(w(1, 80.0, 1), w(2, 79.0, 2), w(3, 79.5, 3)))

        assertEquals(listOf(3L, 2L, 1L), state.items.map { it.weight.id })
        assertEquals(0.5, state.items[0].change!!, 1e-9)
        assertEquals(-1.0, state.items[1].change!!, 1e-9)
        assertNull(state.items[2].change)
    }

    @Test
    fun `sira bozuk gelse de tarihe gore siralar`() {
        val state = buildHomeState(listOf(w(2, 79.0, 5), w(1, 80.0, 1)))

        assertEquals(2L, state.latest!!.id)
        assertEquals(listOf(80.0, 79.0), state.trend)
    }

    @Test
    fun `haftalik degisim en az 7 gun onceki en yeni kayda gore hesaplanir`() {
        val state = buildHomeState(listOf(w(1, 82.0, 0), w(2, 81.0, 2), w(3, 80.0, 8), w(4, 79.5, 10)))

        // Son kayıt 10. gün; 7+ gün önceki en yeni kayıt 2. gün (81.0).
        assertEquals(-1.5, state.weeklyChange!!, 1e-9)
    }

    @Test
    fun `yedi gunluk gecmis yoksa haftalik degisim yoktur`() {
        val state = buildHomeState(listOf(w(1, 80.0, 0), w(2, 79.0, 3)))

        assertNull(state.weeklyChange)
    }

    @Test
    fun `mini grafik son on kayitla sinirlidir`() {
        val state = buildHomeState((1..15).map { w(it.toLong(), 70.0 + it, it) })

        assertEquals(10, state.trend.size)
        assertEquals(76.0, state.trend.first(), 1e-9)
        assertEquals(85.0, state.trend.last(), 1e-9)
    }
}
