package com.yusufcanmercan.weight_track_app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeyirLogicTest {
    private fun point(day: Int, kg: Double) = DailyWeight(day * DAY_MILLIS, kg, kg)

    @Test
    fun `weekly summary counts only unique days inside the seven day window`() {
        val summary = weeklySummary(listOf(point(1, 80.0), point(3, 79.0), point(7, 78.0), point(10, 76.0)), 8 * DAY_MILLIS)

        assertEquals(2 * DAY_MILLIS, summary.from)
        assertEquals(listOf(3 * DAY_MILLIS, 7 * DAY_MILLIS), summary.days.map { it.dayMillis })
        assertEquals(-2.0, summary.change!!, 0.0)
    }

    @Test
    fun `weekly display treats tiny changes as unsigned zero without affecting comparisons`() {
        val end = 8 * DAY_MILLIS
        val previous = point(1, 80.0)
        for (delta in listOf(-0.04, 0.0, 0.04)) {
            val summary = weeklySummary(listOf(previous, point(8, 80.0 + delta)), end)
            assertEquals(delta, summary.change!!, 1e-9)
            assertEquals(0.0, summary.displayChange!!, 0.0)
        }
        assertEquals(0.06, weeklySummary(listOf(previous, point(8, 80.06)), end).displayChange!!, 1e-9)
        assertNull(weeklySummary(listOf(point(8, 80.0)), end).displayChange)
    }

    @Test
    fun `streak includes yesterday and ends at the first gap`() {
        val weights = listOf(point(1, 80.0), point(4, 79.0), point(5, 78.0), point(8, 76.0))

        assertEquals(2, currentStreak(weights, 6 * DAY_MILLIS))
        assertEquals(0, currentStreak(weights, 7 * DAY_MILLIS))
        assertEquals(1, currentStreak(weights, 8 * DAY_MILLIS))
    }

    @Test
    fun `forecast follows a four week trend only when it leads toward the goal`() {
        val days = listOf(point(0, 80.0), point(28, 76.0))
        val toward = progressMetrics(days, 80.0, goalProgress(80.0, 76.0, 72.0))!!
        val away = progressMetrics(days, 80.0, goalProgress(80.0, 76.0, 84.0))!!

        assertEquals(-1.0, toward.weeklyPace!!, 0.0)
        assertEquals(56 * DAY_MILLIS, toward.arrivalDay)
        assertNull(away.arrivalDay)
    }

    @Test
    fun `milestones respect gain targets and remember dates reached`() {
        val days = listOf(point(0, 60.0), point(7, 62.1), point(10, 61.5))
        val milestones = buildMilestones(days, 60.0, 65.0, null)

        assertEquals(2.0, milestones.first().amount, 0.0)
        assertEquals(7 * DAY_MILLIS, milestones.first().reachedOn)
        assertEquals(MilestoneKind.GOAL, milestones.last().kind)
        assertTrue(milestones.drop(1).all { it.reachedOn == null })
    }
}
