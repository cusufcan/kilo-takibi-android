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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val kgRange = (data.maxKg - data.minKg).toFloat()
        // Tek kayıtta maxDay 0 olur; sıfıra bölmemek için nokta ortaya konur.
        fun x(day: Long) = if (data.maxDay == 0L) size.width / 2 else size.width * day / data.maxDay
        fun y(kg: Double) = size.height * ((data.maxKg - kg).toFloat() / kgRange)

        drawLine(axisColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2f)
        drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)

        val path = Path()
        data.points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(x(p.day), y(p.kg)) else path.lineTo(x(p.day), y(p.kg))
        }
        drawPath(path, lineColor, style = Stroke(width = 4f))
        data.points.forEach { drawCircle(lineColor, radius = 8f, center = Offset(x(it.day), y(it.kg))) }
    }
}
