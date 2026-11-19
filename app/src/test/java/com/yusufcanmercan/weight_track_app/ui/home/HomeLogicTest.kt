package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

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
    fun `satirlar yerel aya gore gruplanir en yeni ay ustte`() {
        val utc = TimeZone.getTimeZone("UTC")
        // 1970: 30 Ocak, 31 Ocak, 1 Şubat, 15 Şubat (gün 0 = 1 Ocak).
        val items = buildHomeState(listOf(w(1, 80.0, 29), w(2, 79.0, 30), w(3, 78.0, 31), w(4, 77.0, 45))).items

        val months = groupByMonth(items, utc)

        assertEquals(listOf(1970 to 1, 1970 to 0), months.map { it.year to it.month })
        assertEquals(listOf(4L, 3L), months[0].items.map { it.weight.id })
        assertEquals(listOf(2L, 1L), months[1].items.map { it.weight.id })
    }

    @Test
    fun `ay sinirinda saat dilimi dikkate alinir`() {
        // 31 Ocak 23:00 UTC = İstanbul'da 1 Şubat 02:00.
        val item = HomeItem(Weight(1, 80.0, 30 * day + 23 * 60 * 60 * 1000L), null)

        assertEquals(0, groupByMonth(listOf(item), TimeZone.getTimeZone("UTC")).single().month)
        assertEquals(1, groupByMonth(listOf(item), TimeZone.getTimeZone("Europe/Istanbul")).single().month)
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
