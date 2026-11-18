package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import kotlin.math.abs
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.ui.theme.LocalChangeColors
import java.text.DateFormat
import java.util.Date

/** ViewModel'i bağlayan ince kabuk. Çizimi `HomeContent` yapar. */
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onNewGoal: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(state = state, onEdit = onEdit, onDelete = viewModel::onDelete, onNewGoal = onNewGoal)
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_title))
        }
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onEdit: (Long) -> Unit,
    onDelete: (Weight) -> Unit,
    onNewGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoading && state.items.isEmpty()) {
        EmptyState(modifier = modifier.fillMaxSize())
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Alt boşluk: son kart FAB'ın altında kalmasın.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.latest?.let { latest ->
            item(key = "current") { CurrentWeightCard(latest, state.weeklyChange, state.trend, state.goal, onNewGoal) }
        }
        items(state.items, key = { it.weight.id }) { item ->
            WeightCard(item = item, onClick = { onEdit(item.weight.id) }, onDelete = { onDelete(item.weight) })
        }
    }
}

@Composable
private fun CurrentWeightCard(latest: Weight, weeklyChange: Double?, trend: List<Double>, goal: GoalProgress?, onNewGoal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.home_current), style = MaterialTheme.typography.labelLarge)
            Text(
                text = stringResource(R.string.weight_kg, latest.weight),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (weeklyChange != null) {
                    stringResource(R.string.home_week_change, weeklyChange)
                } else {
                    stringResource(R.string.home_week_no_data)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (goal != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (goal.reached) {
                        // Hedefin 0,1 kg'dan fazla ötesindeysek ne kadar aştığını da söyle.
                        when {
                            abs(goal.remaining) < 0.1 -> stringResource(R.string.home_goal_reached)
                            goal.remaining < 0 -> stringResource(R.string.home_goal_reached_below, abs(goal.remaining))
                            else -> stringResource(R.string.home_goal_reached_above, goal.remaining)
                        }
                    } else {
                        stringResource(R.string.home_goal_remaining, abs(goal.remaining))
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { goal.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                )
                if (goal.reached) {
                    TextButton(
                        onClick = onNewGoal,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                    ) { Text(stringResource(R.string.home_goal_new)) }
                }
            }
            if (trend.size >= 2) {
                Spacer(Modifier.height(12.dp))
                MiniTrend(
                    values = trend,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                )
            }
        }
    }
}

/** Kart içindeki küçük çizgi: eksen yok, yalnız son kiloların eğilimi. */
@Composable
private fun MiniTrend(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val min = values.min()
        val range = (values.max() - min).takeIf { it > 0.0 } ?: 1.0
        val pad = 4.dp.toPx()
        fun point(i: Int) = Offset(
            x = pad + (size.width - 2 * pad) * i / (values.size - 1),
            y = pad + (size.height - 2 * pad) * (1f - ((values[i] - min) / range).toFloat()),
        )
        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until values.size) lineTo(point(i).x, point(i).y)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(color, radius = 4.dp.toPx(), center = point(values.lastIndex))
    }
}

@Composable
private fun WeightCard(item: HomeItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.weight_kg, item.weight.weight),
                    style = MaterialTheme.typography.titleMedium,
                )
                // Gün bazında gösterim: saat kısmı anlamsız (eski kodda rastgele).
                Text(
                    text = DateFormat.getDateInstance().format(Date(item.weight.timeStamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.change?.let { ChangeBadge(it) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}

/** Fark rozeti: verilen kilo = yeşil, alınan/aynı = mor (ADR-0010). */
@Composable
private fun ChangeBadge(change: Double) {
    val colors = LocalChangeColors.current
    val (container, content) = if (change < 0) {
        colors.decreaseContainer to colors.onDecreaseContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = stringResource(R.string.change_kg, change),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ScaleIllustration()
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Basit bir baskül çizimi: yuvarlak köşeli mor kare, içinde açık kadran ve ibre. */
@Composable
private fun ScaleIllustration() {
    val body = MaterialTheme.colorScheme.primary
    val face = MaterialTheme.colorScheme.primaryContainer
    val needle = MaterialTheme.colorScheme.onPrimaryContainer
    Canvas(modifier = Modifier.size(140.dp)) {
        drawRoundRect(body, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(36.dp.toPx()))
        val dial = Size(size.width * 0.6f, size.height * 0.6f)
        val topLeft = Offset((size.width - dial.width) / 2, (size.height - dial.height) / 2)
        drawOval(face, topLeft = topLeft, size = dial)
        val center = Offset(size.width / 2, size.height / 2)
        drawLine(
            needle,
            start = center,
            end = Offset(center.x + dial.width * 0.28f, center.y - dial.height * 0.2f),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(needle, radius = 6.dp.toPx(), center = center)
    }
}
