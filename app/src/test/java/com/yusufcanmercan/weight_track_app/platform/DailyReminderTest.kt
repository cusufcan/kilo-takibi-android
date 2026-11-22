package com.yusufcanmercan.weight_track_app.platform

import com.yusufcanmercan.weight_track_app.ui.home.DailyWeight
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyReminderTest {
    private val day = 24 * 60 * 60 * 1000L
    private val hour = 60 * 60 * 1000L
    private val utc = TimeZone.getTimeZone("UTC")
    private fun d(dayNo: Int) = DailyWeight(dayNo * day, 80.0, 80.0)

    @Test
    fun `saat gecmediyse bugun gectiyse yarin kurulur`() {
        val now = 10 * day + 7 * hour
        assertEquals(10 * day + 8 * hour, nextReminderAt(now, 8 * 60, utc))
        assertEquals(11 * day + 6 * hour + 30 * 60_000, nextReminderAt(now, 6 * 60 + 30, utc))
        // Tam saatinde çalışan alarm aynı ana yeniden kurulmaz.
        assertEquals(11 * day + 8 * hour, nextReminderAt(10 * day + 8 * hour, 8 * 60, utc))
    }

    @Test
    fun `saat yerel saat dilimine gore hesaplanir`() {
        // 2024'te İstanbul sürekli UTC+3: yerel 08:00 = 05:00 UTC (gün 20000 = 4 Ekim 2024).
        val istanbul = TimeZone.getTimeZone("Europe/Istanbul")
        assertEquals(20000 * day + 5 * hour, nextReminderAt(20000 * day, 8 * 60, istanbul))
    }

    @Test
    fun `bugun kayit varsa bildirim gosterilmez`() {
        assertEquals(ReminderMessage.Skip, reminderMessage(listOf(d(9), d(10)), 10 * day))
    }

    @Test
    fun `seri varsa seri metni yoksa duz metin`() {
        assertEquals(ReminderMessage.Streak(3), reminderMessage(listOf(d(7), d(8), d(9)), 10 * day))
        assertEquals(ReminderMessage.Plain, reminderMessage(listOf(d(9)), 10 * day))
        assertEquals(ReminderMessage.Plain, reminderMessage(listOf(d(5), d(6)), 10 * day))
        assertEquals(ReminderMessage.Plain, reminderMessage(emptyList(), 10 * day))
    }
}
