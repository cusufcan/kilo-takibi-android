package com.yusufcanmercan.weight_track_app.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import java.text.DateFormat
import java.util.Date

@Composable
fun GraphScreen(viewModel: GraphViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GraphContent(state)
}

@Composable
fun GraphContent(state: GraphUiState, modifier: Modifier = Modifier) {
    val data = state.data
    when {
        state.isLoading -> Unit
        data == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.graph_empty))
        }
        else -> Column(modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.graph_change, data.change),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                // Sol: Y ekseni etiketleri (üstte en yüksek, altta en düşük kilo).
                Column(
                    modifier = Modifier.height(CHART_HEIGHT),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("%.0f".format(data.maxKg), style = MaterialTheme.typography.labelSmall)
                    Text("%.0f".format(data.minKg), style = MaterialTheme.typography.labelSmall)
                }
                Chart(data, Modifier.weight(1f).height(CHART_HEIGHT).padding(start = 8.dp))
            }
            // Alt: X ekseni etiketleri (ilk ve son kayıt tarihi).
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val format = DateFormat.getDateInstance(DateFormat.SHORT)
                Text(format.format(Date(data.startMillis)), style = MaterialTheme.typography.labelSmall)
                Text(
                    format.format(Date(data.startMillis + (data.maxDay * DAY_MS).toLong())),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private val CHART_HEIGHT = 240.dp

@Composable
private fun Chart(data: ChartData, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer
    val bubbleTextStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
    val goalColor = MaterialTheme.colorScheme.secondary
    val goalTextStyle = MaterialTheme.typography.labelSmall.copy(color = goalColor)
    val goalLabel = data.goalKg?.let { stringResource(R.string.graph_goal_label, it) }.orEmpty()
    val measurer = rememberTextMeasurer()
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    // Dokunulan noktanın sırası; veri değişince sıfırlanır.
    var selected by remember(data) { mutableStateOf<Int?>(null) }

    Canvas(
        modifier
            .pointerInput(data) {
                detectTapGestures { selected = nearestPointIndex(data, it.x / size.width) }
            }
            .pointerInput(data) {
                detectHorizontalDragGestures { change, _ ->
                    selected = nearestPointIndex(data, change.position.x / size.width)
                }
            },
    ) {
        val kgRange = (data.maxKg - data.minKg).toFloat()
        // Tek kayıtta maxDay 0 olur; sıfıra bölmemek için nokta ortaya konur.
        fun x(day: Long) = if (data.maxDay == 0L) size.width / 2 else size.width * day / data.maxDay
        fun y(kg: Double) = size.height * ((data.maxKg - kg).toFloat() / kgRange)

        // Yatay ızgara: üst, alt ve aradaki 3 çizgi.
        repeat(GRID_LINES) { i ->
            val gy = size.height * i / (GRID_LINES - 1)
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f)
        }

        // Hedef: kesikli yatay çizgi + etiket (çizgi eksen aralığına dahil, bkz. buildChartData).
        data.goalKg?.let { goal ->
            val gy = y(goal)
            drawLine(
                goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
            )
            val label = measurer.measure(goalLabel, goalTextStyle)
            val top = if (gy - label.size.height - 6f < 0f) gy + 6f else gy - label.size.height - 6f
            drawText(label, topLeft = Offset(size.width - label.size.width - 8f, top))
        }

        val line = Path()
        data.points.forEachIndexed { i, p ->
            if (i == 0) line.moveTo(x(p.day), y(p.kg)) else line.lineTo(x(p.day), y(p.kg))
        }
        // Çizginin altını kapatıp yukarıdan aşağı sönen gradyanla doldur.
        val fill = Path().apply {
            addPath(line)
            lineTo(x(data.points.last().day), size.height)
            lineTo(x(data.points.first().day), size.height)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0f))),
        )
        drawPath(line, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        data.points.forEach { drawCircle(lineColor, radius = 8f, center = Offset(x(it.day), y(it.kg))) }

        selected?.let { i ->
            val p = data.points[i]
            val center = Offset(x(p.day), y(p.kg))
            drawLine(lineColor.copy(alpha = 0.4f), Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 2f)
            drawCircle(lineColor, radius = 14f, center = center)
            val label = "%.1f kg · %s".format(
                p.kg,
                dateFormat.format(Date(data.startMillis + (p.day * DAY_MS).toLong())),
            )
            val text = measurer.measure(label, bubbleTextStyle)
            val padX = 16f
            val padY = 10f
            val w = text.size.width + 2 * padX
            val h = text.size.height + 2 * padY
            // Balon noktanın üstünde; kenarlardan taşmasın diye sıkıştırılır.
            val left = (center.x - w / 2).coerceIn(0f, size.width - w)
            val top = (center.y - h - 24f).coerceAtLeast(0f)
            drawRoundRect(bubbleColor, Offset(left, top), Size(w, h), CornerRadius(h / 2))
            drawText(text, topLeft = Offset(left + padX, top + padY))
        }
    }
}

private const val GRID_LINES = 5
