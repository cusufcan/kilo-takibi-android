package com.yusufcanmercan.weight_track_app.data.export

import com.yusufcanmercan.weight_track_app.data.local.Weight
import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightCsvTest {
    private val zone = TimeZone.getTimeZone("Europe/Istanbul")

    @Test
    fun `yerel tarih ve saatle siralar eski kayitlara saat eklemez`() {
        val midnight = Instant.parse("2026-11-20T21:00:00Z").toEpochMilli()
        val morning = Instant.parse("2026-11-21T04:15:00Z").toEpochMilli()
        val records = listOf(
            Weight(3, 75.125, morning, note = "Ölçüm, \"sabah\"\nİkinci satır", mood = "CALM"),
            Weight(2, 75.4, midnight, hasMeasurementTime = true),
            Weight(1, 75.6, midnight),
        )

        val csv = buildString { writeWeightCsv(this, records, zone) }

        assertEquals(
            "\uFEFFdate,time,weight_kg,mood,note\r\n" +
                "2026-11-21,,75.6,,\r\n" +
                "2026-11-21,00:00:00,75.4,,\r\n" +
                "2026-11-21,07:15:00,75.125,\"CALM\",\"Ölçüm, \"\"sabah\"\"\nİkinci satır\"\r\n",
            csv,
        )
    }

    @Test
    fun `formul gibi baslayan notu ve kesme isaretini geri alinabilir bicimde kodlar`() {
        val records = listOf(
            Weight(1, 76.0, 0L, note = "\t=1+2"),
            Weight(2, 77.0, 0L, note = "'Ölçüm"),
        )

        val csv = buildString { writeWeightCsv(this, records, TimeZone.getTimeZone("UTC")) }

        assertEquals(
            "\uFEFFdate,time,weight_kg,mood,note\r\n" +
                "1970-01-01,,76.0,,\"'\t=1+2\"\r\n" +
                "1970-01-01,,77.0,,\"''Ölçüm\"\r\n",
            csv,
        )
    }
}
