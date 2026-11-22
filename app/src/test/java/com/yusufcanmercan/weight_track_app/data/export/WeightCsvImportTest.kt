package com.yusufcanmercan.weight_track_app.data.export

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.local.hasKnownMeasurementTime
import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightCsvImportTest {
    private val zone = TimeZone.getTimeZone("Europe/Istanbul")
    private val now = Instant.parse("2026-11-22T12:00:00Z").toEpochMilli()
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()
    private fun plan(text: String, existing: List<Weight> = emptyList()) = planWeightCsvImport(text, existing, now, zone)

    @Test
    fun `kendi disa aktarimi geri okunur`() {
        val records = listOf(
            Weight(1, 75.6, at("2026-11-20T21:00:00Z")),
            Weight(2, 75.4, at("2026-11-20T21:00:00Z"), hasMeasurementTime = true),
            Weight(3, 75.125, at("2026-11-21T04:15:00Z"), note = "Ölçüm, \"sabah\"\nİkinci satır", mood = "CALM"),
            Weight(4, 74.9, at("2026-11-21T05:00:00Z"), note = "=1+1"),
            Weight(5, 74.8, at("2026-11-21T06:00:00Z"), note = "'alıntı"),
        )
        val csv = buildString { writeWeightCsv(this, records, zone) }

        val result = plan(csv)

        assertEquals(emptyList<Int>(), result.badLines)
        assertEquals(0, result.duplicates)
        // Saat bilgisi olan eski kayıtlar artık açıkça "saatli" işaretlenir; anlamı aynıdır.
        assertEquals(records.map { it.copy(id = 0, hasMeasurementTime = it.hasKnownMeasurementTime(zone)) }, result.added)
        // Aynı dosyayı ikinci kez almak hiçbir şey eklemez.
        assertEquals(5, plan(csv, records).duplicates)
        assertTrue(plan(csv, records).added.isEmpty())
    }

    @Test
    fun `basit bicim noktali virgul ve ondalik virgulle okunur`() {
        val result = plan("tarih;kilo\n21.11.2026;80,5\n2026-11-20;81\n")

        assertEquals(listOf(81.0, 80.5), result.added.map { it.weight }.sorted().reversed())
        assertTrue(result.added.none { it.hasMeasurementTime })
        assertEquals(at("2026-11-20T21:00:00Z"), result.added.first { it.weight == 80.5 }.timeStamp)
    }

    @Test
    fun `basliksiz ve taninmayan baslikli dosya konumla okunur`() {
        assertEquals(1, plan("2026-11-20,80").added.size)
        assertEquals(1, plan("Day,Value\n2026-11-20,80").added.size)
    }

    @Test
    fun `ayni gun farkli saatler ayri kayittir saatsiz ayni kilo zaten vardir`() {
        val existing = listOf(Weight(1, 80.0, at("2026-11-20T21:00:00Z")))
        val result = plan(
            "date,time,weight_kg\n" +
                "2026-11-21,,80.0\n" +
                "2026-11-21,08:00,80.0\n" +
                "2026-11-21,08:00,80.0\n" +
                "2026-11-21,19:30,80.0\n" +
                "2026-11-21,,79.0\n",
            existing,
        )

        // Mevcut kayıt saatsiz: o günün bütün 80'leri (saatli ya da değil) zaten var sayılır.
        assertEquals(4, result.duplicates)
        assertEquals(listOf(79.0), result.added.map { it.weight })
    }

    @Test
    fun `ikisi de saatliyse ayni gun farkli saat eklenir`() {
        val existing = listOf(Weight(1, 80.0, at("2026-11-21T05:00:00Z"), hasMeasurementTime = true))
        val result = plan("date,time,weight_kg\n2026-11-21,08:00,80\n2026-11-21,19:30,80\n", existing)

        assertEquals(1, result.duplicates)
        assertEquals(at("2026-11-21T16:30:00Z"), result.added.single().timeStamp)
        assertTrue(result.added.single().hasMeasurementTime)
    }

    @Test
    fun `gecersiz satirlarin numarasi bildirilir`() {
        val result = plan(
            "date,time,weight_kg\n" +
                "2026-11-20,,0.5\n" + // 2: sınır altı
                "2026-11-23,,80\n" + // 3: gelecek
                "2026-02-31,,80\n" + // 4: takvimde yok
                "2026-11-20,25:00,80\n" + // 5: saat bozuk
                "dün,,80\n" + // 6: tarih bozuk
                "\n" + // 7: boş, atlanır
                "2026-11-20,,abc\n", // 8: kilo bozuk
        )

        assertEquals(listOf(2, 3, 4, 5, 6, 8), result.badLines)
        assertTrue(result.added.isEmpty())
    }

    @Test
    fun `tirnak icindeki satir sonu satir numarasini kaydirir`() {
        val result = plan("date,time,weight_kg,mood,note\n2026-11-20,,80,,\"a\nb\"\n2026-11-20,,x,,\n")

        assertEquals(listOf(4), result.badLines)
        assertEquals("a\nb", result.added.single().note)
    }

    @Test
    fun `gecersiz ruh hali atilir uzun not kisaltilir`() {
        val result = plan("date,weight_kg,mood,note\n2026-11-20,80,HAPPY,${"n".repeat(300)}\n")

        assertNull(result.added.single().mood)
        assertEquals(200, result.added.single().note!!.length)
        assertFalse(result.added.single().hasMeasurementTime)
    }
}
