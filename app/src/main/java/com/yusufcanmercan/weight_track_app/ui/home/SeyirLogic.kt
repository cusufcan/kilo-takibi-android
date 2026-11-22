package com.yusufcanmercan.weight_track_app.ui.home

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

internal val Utc = TimeZone.getTimeZone("UTC")
internal const val WEEK_DAYS = 7
private const val PACE_DAYS = 28
private const val MAX_FORECAST_DAYS = 3650
private const val HALF_DISPLAY_STEP_KG = 0.05

internal data class WeeklySummary(
    val from: Long,
    val to: Long,
    val days: List<DailyWeight>,
    val change: Double?,
) {
    val displayChange: Double?
        get() = change?.let { if (abs(it) < HALF_DISPLAY_STEP_KG) 0.0 else it }
}

internal fun weeklySummary(weights: List<DailyWeight>, throughDay: Long): WeeklySummary {
    val from = throughDay - (WEEK_DAYS - 1) * DAY_MILLIS
    val days = weights.filter { it.dayMillis in from..throughDay }
    val previous = weights.lastOrNull { it.dayMillis < from }
    val current = days.lastOrNull()
    return WeeklySummary(from, throughDay, days, if (previous != null && current != null) current.trendKg - previous.trendKg else null)
}

/** Günlük noktalar eskiden yeniye sıralıdır ve her gün tek bir nokta içerir. */
internal fun currentStreak(weights: List<DailyWeight>, throughDay: Long): Int {
    val lastIndex = weights.indexOfLast { it.dayMillis <= throughDay }
    if (lastIndex < 0 || weights[lastIndex].dayMillis < throughDay - DAY_MILLIS) return 0
    var expectedDay = weights[lastIndex].dayMillis
    var streak = 0
    for (index in lastIndex downTo 0) {
        if (weights[index].dayMillis != expectedDay) break
        streak++
        expectedDay -= DAY_MILLIS
    }
    return streak
}

internal data class ProgressMetrics(val total: Double, val weeklyPace: Double?, val arrivalDay: Long?)

internal fun progressMetrics(weights: List<DailyWeight>, startingWeight: Double, goal: GoalProgress?): ProgressMetrics? {
    val current = weights.lastOrNull() ?: return null
    val earlier = weights.lastOrNull { it.dayMillis <= current.dayMillis - PACE_DAYS * DAY_MILLIS }
    val dailyPace = earlier?.let { (current.trendKg - it.trendKg) / ((current.dayMillis - it.dayMillis) / DAY_MILLIS) }
    val distance = goal?.let { it.goalKg - current.trendKg }
    val arrival = if (goal?.reached == true) current.dayMillis else {
        if (distance != null && dailyPace != null && distance * dailyPace > 0) {
            (distance / dailyPace).roundToLong().takeIf { it in 1..MAX_FORECAST_DAYS }
                ?.let { current.dayMillis + it * DAY_MILLIS }
        } else null
    }
    return ProgressMetrics(current.trendKg - startingWeight, dailyPace?.times(WEEK_DAYS), arrival)
}

internal enum class MilestoneKind { KILOGRAMS, PERCENT, BMI, GOAL }
internal data class Milestone(val kind: MilestoneKind, val amount: Double, val reachedOn: Long?, val progress: Float)

internal fun buildMilestones(
    weights: List<DailyWeight>,
    start: Double,
    goalWeight: Double,
    heightCm: Double?,
): List<Milestone> {
    val direction = if (goalWeight < start) -1 else 1
    val goalDistance = abs(goalWeight - start)
    val thresholds = buildList {
        listOf(2.0, 5.0, 8.0).filter { it < goalDistance }.forEach { add(Triple(it, MilestoneKind.KILOGRAMS, it * direction)) }
        val percentDistance = start * 0.05
        if (percentDistance < goalDistance) add(Triple(percentDistance, MilestoneKind.PERCENT, 5.0))
        if (heightCm != null) {
            val initialCategory = calculateBmi(start, heightCm)?.category
            val meters = heightCm / 100
            val boundary = if (direction < 0) Math.nextDown(25.0 * meters * meters) else 18.5 * meters * meters
            val distance = (boundary - start) * direction
            if (initialCategory != BmiCategory.NORMAL && distance > 0 && distance < goalDistance) {
                add(Triple(distance, MilestoneKind.BMI, 0.0))
            }
        }
    }.sortedBy { it.first } + Triple(goalDistance, MilestoneKind.GOAL, goalWeight)

    val best = weights.maxOfOrNull { (it.trendKg - start) * direction } ?: 0.0
    return thresholds.map { (distance, kind, label) ->
        val reached = weights.firstOrNull { (it.trendKg - start) * direction >= distance }?.dayMillis
        val fraction = if (reached != null) 1f else if (distance > 0) (best / distance).toFloat().coerceIn(0f, 1f) else 0f
        Milestone(kind, label, reached, fraction)
    }
}

internal fun firstOfMonth(day: Long): Long = Calendar.getInstance(Utc).apply {
    timeInMillis = day
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

internal fun nextMonth(month: Long, delta: Int): Long = Calendar.getInstance(Utc).apply {
    timeInMillis = month
    add(Calendar.MONTH, delta)
}.timeInMillis
