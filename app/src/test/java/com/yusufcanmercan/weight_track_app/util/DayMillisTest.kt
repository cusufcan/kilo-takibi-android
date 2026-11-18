package com.yusufcanmercan.weight_track_app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class DayMillisTest {
    private val istanbul = TimeZone.getTimeZone("Europe/Istanbul") // UTC+3

    // 18 Kas 2026 12:30 UTC = 15:30 İstanbul
    private val afternoon = 1_795_005_000_000L
    private val istanbulMidnight = 1_794_949_200_000L // 18 Kas 00:00 İstanbul = 17 Kas 21:00 UTC
    private val pickerNov18 = 1_794_960_000_000L // 18 Kas 00:00 UTC

    @Test
    fun `gun basi yerel gece yarisidir`() {
        assertEquals(istanbulMidnight, startOfDay(afternoon, istanbul))
    }

    @Test
    fun `ayni gunun farkli saatleri ayni gun basina gelir`() {
        assertEquals(startOfDay(afternoon, istanbul), startOfDay(afternoon + 3_600_000L, istanbul))
    }

    @Test
    fun `yerel gun secici icin UTC gece yarisina cevrilir`() {
        assertEquals(pickerNov18, toPickerMillis(istanbulMidnight, istanbul))
    }

    @Test
    fun `seciciden gelen gun yerel gece yarisina doner`() {
        assertEquals(istanbulMidnight, fromPickerMillis(pickerNov18, istanbul))
    }
}
