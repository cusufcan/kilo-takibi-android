package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `hedef yoksa goal null`() {
        assertNull(buildHomeState(listOf(w(1, 80.0, 1))).goal)
    }

    @Test
    fun `kilo verirken ilerleme baslangictan hedefe oranlanir`() {
        // 80 -> 70 hedef, şu an 75: yolun yarısı.
        val g = goalProgress(startKg = 80.0, currentKg = 75.0, goalKg = 70.0)

        assertEquals(0.5f, g.progress, 1e-6f)
        assertEquals(5.0, g.remaining, 1e-9)
        assertFalse(g.reached)
    }

    @Test
    fun `kilo alirken de ulasma ve ilerleme dogru`() {
        val half = goalProgress(startKg = 60.0, currentKg = 65.0, goalKg = 70.0)
        val done = goalProgress(startKg = 60.0, currentKg = 71.0, goalKg = 70.0)

        assertEquals(0.5f, half.progress, 1e-6f)
        assertFalse(half.reached)
        assertEquals(1f, done.progress, 1e-6f)
        assertTrue(done.reached)
    }

    @Test
    fun `yanlis yone gidince ilerleme sifirda kalir`() {
        val g = goalProgress(startKg = 80.0, currentKg = 83.0, goalKg = 70.0)

        assertEquals(0f, g.progress, 1e-6f)
        assertFalse(g.reached)
    }

    @Test
    fun `baslangic hedefe esitse bolme hatasi olmaz`() {
        assertTrue(goalProgress(70.0, 70.0, 70.0).reached)
        val off = goalProgress(70.0, 72.0, 70.0)
        assertFalse(off.reached)
        assertEquals(0f, off.progress, 1e-6f)
    }

    @Test
    fun `buildHomeState hedefi ilk kayda gore hesaplar`() {
        val state = buildHomeState(listOf(w(1, 80.0, 1), w(2, 75.0, 2)), goalKg = 70.0)

        assertEquals(0.5f, state.goal!!.progress, 1e-6f)
    }
}
