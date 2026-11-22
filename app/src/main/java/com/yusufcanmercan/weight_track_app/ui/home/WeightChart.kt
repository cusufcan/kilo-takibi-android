package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainWidth
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.theme.LocalSeyirColors
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.ui.theme.seyirCurve
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

private val CHART_HEIGHT = 128.dp
private const val GRID_LINES = 3

/** Tartı noktaları ve eğilim çizgisi aynı takvim ekseninde gösterilir. */
@Composable
fun WeightChart(
    data: ChartData,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = CHART_HEIGHT,
    compact: Boolean = false,
    interactive: Boolean = true,
) {
    val locale = LocalLocale.current.platformLocale
    val shortDate = remember(locale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
        SimpleDateFormat(pattern, locale).apply { timeZone = Utc }
    }
    val fullDate = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { timeZone = Utc } }
    val ticks = remember(data.minKg, data.maxKg, compact) {
        if (compact) emptyList() else kilogramTicks(data.minKg, data.maxKg)
    }
    var selected by remember(data) { mutableStateOf<Int?>(null) }
    val selectPoint: ((Int) -> Unit)? = if (interactive) { index -> selected = index } else null
    Column(modifier) {
        if (!compact) AxisLabel("kg")
        Row(Modifier.fillMaxWidth()) {
            if (!compact) {
                KilogramAxis(ticks, data, Modifier.height(height).padding(end = Spacing.sm))
            }
            Column(Modifier.weight(1f)) {
                Chart(data, ticks, Modifier.fillMaxWidth().height(height), selected, selectPoint)
                Row(
                    Modifier.fillMaxWidth().padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AxisLabel(shortDate.format(Date(data.startMillis)))
                    if (data.forecast != null) {
                        AxisLabel(shortDate.format(Date(data.startMillis + data.points.last().day * DAY_MILLIS)))
                    }
                    AxisLabel(shortDate.format(Date(data.startMillis + data.maxDay * DAY_MILLIS)))
                }
            }
        }
        if (interactive) {
            val index = selected ?: data.points.lastIndex
            val point = data.points[index]
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selected = index - 1 }, enabled = index > 0) {
                    SeyirIcon(R.drawable.ic_seyir_prev, description = stringResource(R.string.chart_previous_measurement))
                }
                Column(
                    Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        fullDate.format(Date(data.startMillis + point.day * DAY_MILLIS)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    StatNote(stringResource(R.string.chart_measurement_values, point.measuredKg, point.trendKg))
                }
                IconButton(onClick = { selected = index + 1 }, enabled = index < data.points.lastIndex) {
                    SeyirIcon(R.drawable.ic_seyir_next, description = stringResource(R.string.chart_next_measurement))
                }
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun KilogramAxis(ticks: List<Double>, data: ChartData, modifier: Modifier) {
    val locale = LocalLocale.current.platformLocale
    val format = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 } }
    Layout(content = { ticks.forEach { AxisLabel(format.format(it)) } }, modifier = modifier) { labels, constraints ->
        val height = constraints.maxHeight
        val placeables = labels.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
        layout(width, height) {
            placeables.forEachIndexed { index, label ->
                val y = (height * data.verticalFraction(ticks[index]) - label.height / 2f).roundToInt()
                label.placeRelative(width - label.width, y.coerceIn(0, (height - label.height).coerceAtLeast(0)))
            }
        }
    }
}

private fun ChartData.verticalFraction(kg: Double): Float = ((maxKg - kg) / (maxKg - minKg)).toFloat()

@Composable
private fun Chart(
    data: ChartData,
    ticks: List<Double>,
    modifier: Modifier,
    selected: Int?,
    selectPoint: ((Int) -> Unit)?,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = LocalSeyirColors.current.chartDots
    val cardColor = MaterialTheme.colorScheme.surfaceContainer
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.secondary
    val forecastColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val goalTextStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val forecastLabel = stringResource(R.string.chart_forecast)
    val goalLabel = data.goalKg?.let { stringResource(R.string.graph_goal_label, it) }.orEmpty()
    val measurer = rememberTextMeasurer()
    Canvas(
        if (selectPoint == null) modifier else modifier
            .pointerInput(data) {
                detectTapGestures { selectPoint(nearestPointIndex(data, it.x / size.width)) }
            }
            .pointerInput(data) {
                detectHorizontalDragGestures { change, _ ->
                    selectPoint(nearestPointIndex(data, change.position.x / size.width))
                }
            },
    ) {
        // Tek kayıtta maxDay 0 olur; sıfıra bölmemek için nokta ortaya konur.
        fun x(day: Long) = if (data.maxDay == 0L) size.width / 2 else size.width * day / data.maxDay
        fun y(kg: Double) = size.height * data.verticalFraction(kg)

        val last = data.points.last()
        val lastCenter = Offset(x(last.day), y(last.trendKg))
        data.forecast?.let {
            val forecastWidth = size.width - lastCenter.x
            drawRoundRect(forecastColor, Offset(lastCenter.x, 0f), Size(forecastWidth, size.height), CornerRadius(8.dp.toPx()))
            val label = measurer.measure(forecastLabel, goalTextStyle)
            if (forecastWidth > label.size.width + 12.dp.toPx()) {
                drawText(label, topLeft = Offset(lastCenter.x + 6.dp.toPx(), 6.dp.toPx()))
            }
        }

        val grid = if (ticks.isEmpty()) {
            List(GRID_LINES) { size.height * it / (GRID_LINES - 1) }
        } else ticks.map(::y)
        grid.forEach { gy ->
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
        }

        // Hedef: kesikli yatay çizgi + etiket (çizgi eksen aralığına dahil, bkz. buildChartData).
        data.goalKg?.let { goal ->
            val gy = y(goal)
            drawLine(
                goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
            val label = measurer.measure(goalLabel, goalTextStyle)
            val top = if (gy - label.size.height - 6f < 0f) gy + 6f else gy - label.size.height - 6f
            drawText(label, topLeft = Offset(0f, top))
        }

        val line = Path()
        seyirCurve(data.points.map { Offset(x(it.day), y(it.trendKg)) }, line::moveTo, line::cubicTo)
        drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        data.points.forEach { point ->
            drawCircle(dotColor, radius = 2.dp.toPx(), center = Offset(x(point.day), y(point.measuredKg)))
        }
        data.forecast?.let { forecast ->
            drawLine(
                dotColor,
                lastCenter,
                Offset(x(forecast.day), y(forecast.trendKg)),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
        drawCircle(cardColor, radius = 5.dp.toPx(), center = lastCenter)
        drawCircle(lineColor, radius = 4.dp.toPx(), center = lastCenter)

        selected?.let { i ->
            val p = data.points[i]
            val center = Offset(x(p.day), y(p.measuredKg))
            drawLine(gridColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(lineColor, radius = 5.dp.toPx(), center = center)
        }
    }
}
