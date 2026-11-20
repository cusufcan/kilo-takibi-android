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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

private val CHART_HEIGHT = 200.dp
private const val GRID_LINES = 3

/** Kutusuz, tam genişlik grafik: solda Y, altta X ekseni etiketleri (tasarim.md). */
@Composable
fun WeightChart(data: ChartData, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Chart(data, Modifier.weight(1f).height(CHART_HEIGHT))
            // Sağ: Y ekseni etiketleri (üstte en yüksek, altta en düşük kilo).
            Column(
                modifier = Modifier.height(CHART_HEIGHT).padding(start = Spacing.sm),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel("%.0f".format(data.maxKg))
                AxisLabel("%.0f".format(data.minKg))
            }
        }
        // Alt: ilk ve son kayıt tarihi.
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val format = DateFormat.getDateInstance(DateFormat.SHORT)
            AxisLabel(format.format(Date(data.startMillis)))
            AxisLabel(format.format(Date(data.startMillis + (data.maxDay * DAY_MS).toLong())))
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
private fun Chart(data: ChartData, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bubbleColor = MaterialTheme.colorScheme.inverseSurface
    val bubbleTextStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.inverseOnSurface)
    val goalColor = MaterialTheme.colorScheme.secondary
    val goalTextStyle = MaterialTheme.typography.labelSmall.copy(color = goalColor)
    val goalLabel = data.goalKg?.let { stringResource(R.string.graph_goal_label, it) }.orEmpty()
    val measurer = rememberTextMeasurer()
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    // Dokunulan noktanın sırası; veri (ör. aralık) değişince sıfırlanır.
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

        // Yatay ızgara: üst, orta, alt; ince ve soluk.
        repeat(GRID_LINES) { i ->
            val gy = size.height * i / (GRID_LINES - 1)
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
        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.18f), lineColor.copy(alpha = 0f))))
        drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Yalnız son nokta işaretli: çok kayıtta noktalar çizgiyi boğmasın.
        val last = data.points.last()
        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x(last.day), y(last.kg)))

        selected?.let { i ->
            val p = data.points[i]
            val center = Offset(x(p.day), y(p.kg))
            drawLine(gridColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(lineColor, radius = 5.dp.toPx(), center = center)
            val label = "%.1f kg · %s".format(
                p.kg,
                dateFormat.format(Date(data.startMillis + (p.day * DAY_MS).toLong())),
            )
            val text = measurer.measure(label, bubbleTextStyle)
            val padX = 12.dp.toPx()
            val padY = 6.dp.toPx()
            val w = text.size.width + 2 * padX
            val h = text.size.height + 2 * padY
            // Balon noktanın üstünde; kenarlardan taşmasın diye sıkıştırılır.
            val left = (center.x - w / 2).coerceIn(0f, size.width - w)
            val top = (center.y - h - 12.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(bubbleColor, Offset(left, top), Size(w, h), CornerRadius(h / 2))
            drawText(text, topLeft = Offset(left + padX, top + padY))
        }
    }
}
