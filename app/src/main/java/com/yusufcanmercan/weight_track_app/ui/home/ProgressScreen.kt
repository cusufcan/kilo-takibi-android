package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.ui.theme.LocalSeyirColors
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIconTile
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(state: HomeUiState, onAdd: () -> Unit) {
    var range by rememberSaveable { mutableStateOf(ChartRange.QUARTER) }
    val days = state.dailyWeights
    val start = state.items.lastOrNull()?.weight?.weight
    val metrics = remember(days, start, state.goal) { start?.let { progressMetrics(days, it, state.goal) } }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = Spacing.lg, top = Spacing.xl, end = Spacing.lg, bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle(stringResource(R.string.nav_progress))
        if (state.isLoading) {
            StatNote(stringResource(R.string.loading))
        } else if (days.isEmpty() || metrics == null) {
            FirstEntryCard(R.drawable.ic_seyir_progress, stringResource(R.string.progress_empty), onAdd)
        } else {
            val hasTrend = days.size > 1
            if (hasTrend) RangeSelector(range, onSelect = { range = it })
            AnimatedContent(
                targetState = range,
                transitionSpec = {
                    (fadeIn(tween(120, delayMillis = 40)) togetherWith fadeOut(tween(40)))
                        .using(SizeTransform { _, _ -> tween(160) })
                },
                label = "Progress chart range",
            ) { chartRange ->
                val data = remember(days, chartRange, state.goal?.goalKg, metrics.arrivalDay) {
                    val filtered = filterByRange(days, chartRange)
                    val maxForecastDistance = (chartRange.days ?: 365) * DAY_MILLIS
                    val forecast = metrics.arrivalDay?.takeIf { it - filtered.last().dayMillis <= maxForecastDistance }
                    buildChartData(filtered, state.goal?.goalKg, forecast)
                }
                if (data != null) ProgressChart(data, hasTrend, days.last().trendKg) { range = ChartRange.ALL }
            }
            if (!hasTrend) return@Column
            Text(
                stringResource(R.string.progress_all_records),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Metric(
                    stringResource(R.string.progress_total),
                    stringResource(R.string.change_kg, metrics.total),
                    state.items.lastOrNull()?.dateLabel().orEmpty(),
                    Modifier.weight(1f),
                )
                Metric(
                    stringResource(R.string.progress_pace),
                    metrics.weeklyPace?.let { stringResource(R.string.change_kg, it) } ?: "—",
                    stringResource(if (metrics.weeklyPace == null) R.string.progress_pace_unavailable else R.string.progress_pace_detail),
                    Modifier.weight(1f),
                )
            }
            state.goal?.let { goal ->
                Metric(
                    stringResource(R.string.progress_arrival_heading, goal.goalKg),
                    when {
                        goal.reached -> stringResource(R.string.home_goal_reached)
                        metrics.arrivalDay != null -> formattedDay(metrics.arrivalDay, DateFormat.LONG)
                        metrics.weeklyPace != null -> stringResource(R.string.progress_no_path)
                        else -> stringResource(R.string.progress_no_forecast)
                    },
                    stringResource(R.string.progress_forecast_detail),
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun RangeSelector(selected: ChartRange, onSelect: (ChartRange) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val position by animateFloatAsState(selected.ordinal.toFloat(), tween(150), label = "Selected range")
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .background(colors.surfaceContainerHigh, CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .padding(Spacing.xs),
    ) {
        val ranges = ChartRange.entries
        val segmentWidth = (maxWidth - Spacing.xs * (ranges.size - 1)) / ranges.size
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.offset { IntOffset(((segmentWidth + Spacing.xs) * position).roundToPx(), 0) }
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .background(colors.primary, CircleShape),
            )
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ranges.forEach { range ->
                val interactions = remember { MutableInteractionSource() }
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                        .selectable(range == selected, interactions, indication = null, role = Role.Tab) { onSelect(range) }
                        .indication(interactions, ripple(bounded = false)),
                    contentAlignment = Alignment.Center,
                ) {
                    val label = when (range) {
                        ChartRange.WEEK -> R.string.range_week
                        ChartRange.MONTH -> R.string.range_month
                        ChartRange.QUARTER -> R.string.range_quarter
                        ChartRange.ALL -> R.string.range_all
                    }
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (range.ordinal == position.roundToInt()) colors.onPrimary else colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressChart(chart: ChartData, hasTrend: Boolean, latestTrend: Double, onShowAll: () -> Unit) {
    if (chart.points.size == 1) {
        val point = chart.points.single()
        Card {
            Text(
                stringResource(if (hasTrend) R.string.progress_one_in_range else R.string.first_measurement),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.weight_kg, point.measuredKg),
                modifier = Modifier.padding(top = Spacing.sm),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            StatNote(formattedDay(chart.startMillis + point.day * DAY_MILLIS))
            if (hasTrend) {
                TextButton(onClick = onShowAll) { Text(stringResource(R.string.progress_show_all)) }
            } else {
                StatNote(stringResource(R.string.single_measurement_hint), Modifier.padding(top = Spacing.sm))
            }
        }
    } else Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.home_chart_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.weight_kg, latestTrend),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        WeightChart(chart, Modifier.padding(top = Spacing.md), height = 216.dp)
        ChartLegend(chart.goalKg != null, chart.forecast != null, Modifier.padding(top = Spacing.sm))
    }
}

@Composable
internal fun ScreenTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
internal fun formattedDay(day: Long, style: Int = DateFormat.MEDIUM): String {
    val locale = LocalLocale.current.platformLocale
    return remember(day, style, locale) {
        DateFormat.getDateInstance(style, locale).apply { timeZone = Utc }.format(Date(day))
    }
}

@Composable
private fun HomeItem.dateLabel(): String = formattedDay(com.yusufcanmercan.weight_track_app.util.toPickerMillis(weight.timeStamp))

@Composable
private fun Metric(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Card(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 23.sp, lineHeight = 27.sp),
                    color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = Spacing.xs),
                )
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (emphasized) SeyirIconTile(R.drawable.ic_seyir_target)
        }
    }
}

@Composable
private fun ChartLegend(hasGoal: Boolean, hasForecast: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val dots = LocalSeyirColors.current.chartDots
    val secondary = MaterialTheme.colorScheme.secondary
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ChartLegendItem(R.string.chart_measurements) {
            drawCircle(dots, radius = 3.dp.toPx())
        }
        ChartLegendItem(R.string.chart_trend) {
            drawLine(primary, Offset(0f, center.y), Offset(size.width, center.y), 2.dp.toPx(), StrokeCap.Round)
        }
        if (hasGoal) {
            ChartLegendItem(R.string.chart_goal) {
                drawLine(
                    secondary, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                )
            }
        }
        if (hasForecast) {
            ChartLegendItem(R.string.chart_forecast) {
                drawLine(
                    dots, Offset.Zero, Offset(size.width, size.height), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: Int, drawMarker: DrawScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp, 8.dp), onDraw = drawMarker)
        StatNote(stringResource(label))
    }
}
