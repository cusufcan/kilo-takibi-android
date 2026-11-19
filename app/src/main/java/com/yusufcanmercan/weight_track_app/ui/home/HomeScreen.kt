package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.ui.theme.LocalChangeColors
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

/** ViewModel'i bağlayan ince kabuk. Çizimi `HomeContent` yapar. */
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(state = state, onEdit = onEdit, onDelete = viewModel::onDelete, onOpenSettings = onOpenSettings)
        FloatingActionButton(
            onClick = onAdd,
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_title))
        }
    }
}

/** Son satır FAB'ın altında kalmasın: FAB (56) + iki yanındaki `lg` boşluk. */
private val FabClearance = 56.dp + Spacing.lg * 2

@Composable
fun HomeContent(
    state: HomeUiState,
    onEdit: (Long) -> Unit,
    onDelete: (Weight) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoading && state.items.isEmpty()) {
        EmptyState(modifier = modifier.fillMaxSize())
        return
    }
    // Aynı anda yalnız bir satır açık; döndürmede korunur.
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = FabClearance),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "header") { Header() }
        state.latest?.let { latest ->
            item(key = "hero") {
                HeroCard(latest, state.weeklyChange, state.trend, state.goal, state.bmi, state.hasHeight, onOpenSettings)
            }
        }
        item(key = "historyTitle") {
            // Bölümler arası `xl`: liste aralığı (`sm`) + bu dolgu (`lg`).
            Text(
                text = stringResource(R.string.home_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.lg),
            )
        }
        items(state.items, key = { it.weight.id }) { item ->
            val id = item.weight.id
            WeightRow(
                item = item,
                expanded = expandedId == id,
                onToggle = { expandedId = if (expandedId == id) null else id },
                onEdit = { onEdit(id) },
                onDelete = { onDelete(item.weight) },
            )
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(bottom = Spacing.sm)) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
    }
}

/** Kahraman kart içi yüzeyler: mor üstünde `onPrimary`'nin hafif bir tonu (tasarim.md). */
@Composable
private fun heroInnerColor() = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)

@Composable
private fun HeroCard(
    latest: Weight,
    weeklyChange: Double?,
    trend: List<Double>,
    goal: GoalProgress?,
    bmi: Bmi?,
    hasHeight: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(stringResource(R.string.home_current), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.weight_kg, latest.weight),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (weeklyChange != null) {
                    Spacer(Modifier.size(Spacing.md))
                    Badge(
                        text = stringResource(R.string.home_week_change, weeklyChange),
                        container = heroInnerColor(),
                        content = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            if (weeklyChange == null) {
                Text(
                    text = stringResource(R.string.home_week_no_data),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.alpha(0.8f),
                )
            }
            if (trend.size >= 2) {
                Spacer(Modifier.height(Spacing.md))
                MiniTrend(
                    values = trend,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            // İki kutu aynı yükseklikte: kısa olan uzuna eşitlenir.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                BmiBox(bmi, hasHeight, onOpenSettings, Modifier.weight(1f).fillMaxHeight())
                GoalBox(goal, onOpenSettings, Modifier.weight(1f).fillMaxHeight())
            }
            if (bmi != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.home_bmi_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.alpha(0.7f),
                )
            }
        }
    }
}

/** Kahraman kart içindeki küçük bilgi kutusu: üstte etiket, altta değer ve isteğe bağlı ek. */
@Composable
private fun HeroBox(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = heroInnerColor(),
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(
            modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.alpha(0.8f))
            content()
        }
    }
}

@Composable
private fun BmiBox(bmi: Bmi?, hasHeight: Boolean, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val label = "BMI"
    when {
        bmi != null -> HeroBox(label, modifier) {
            Text("%.1f".format(bmi.value), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(
                    when (bmi.category) {
                        BmiCategory.UNDERWEIGHT -> R.string.bmi_underweight
                        BmiCategory.NORMAL -> R.string.bmi_normal
                        BmiCategory.OVERWEIGHT -> R.string.bmi_overweight
                        BmiCategory.OBESE -> R.string.bmi_obese
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        !hasHeight -> HeroBox(label, modifier, onClick = onOpenSettings) {
            Text(stringResource(R.string.home_bmi_no_height) + " ›", style = MaterialTheme.typography.bodyMedium)
        }
        // Boy var ama BMI hesaplanamadı (ör. veri yükleniyor): boş kutu, düzen kaymasın.
        else -> HeroBox(label, modifier) {}
    }
}

@Composable
private fun GoalBox(goal: GoalProgress?, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    if (goal == null) {
        HeroBox(stringResource(R.string.settings_goal), modifier, onClick = onOpenSettings) {
            Text(stringResource(R.string.home_goal_none) + " ›", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    HeroBox(
        label = stringResource(R.string.home_goal_label, goal.goalKg),
        modifier = modifier,
        onClick = if (goal.reached) onOpenSettings else null,
    ) {
        Text(
            text = if (goal.reached) {
                stringResource(R.string.home_goal_reached)
            } else {
                stringResource(R.string.home_goal_remaining, abs(goal.remaining))
            },
            style = MaterialTheme.typography.titleLarge,
        )
        if (goal.reached) {
            // Hedefin 0,1 kg'dan fazla ötesindeysek ne kadar aştığını da söyle.
            val detail = when {
                abs(goal.remaining) < 0.1 -> null
                goal.remaining < 0 -> stringResource(R.string.home_goal_reached_below, abs(goal.remaining))
                else -> stringResource(R.string.home_goal_reached_above, goal.remaining)
            }
            detail?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            Text(stringResource(R.string.home_goal_new) + " ›", style = MaterialTheme.typography.labelLarge)
        } else {
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = heroInnerColor(),
                drawStopIndicator = {},
            )
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
private fun WeightRow(
    item: HomeItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // Açılırken renk ve köşe yumuşakça değişir; ani sıçrama olmasın. Mor yok: yüzey nötr kalır.
    val container by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(300),
        label = "rowColor",
    )
    val corner by animateDpAsState(if (expanded) 28.dp else 16.dp, tween(300), label = "rowCorner")
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        // animateContentSize kartın İÇİNDE: dışta olsaydı kartı dikdörtgen keser, köşeler keskinleşirdi.
        Column(
            modifier = Modifier
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.weight_kg, item.weight.weight),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    // Gün bazında gösterim: saat kısmı anlamsız (eski kodda rastgele).
                    Text(
                        text = DateFormat.getDateInstance().format(Date(item.weight.timeStamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.change?.let { ChangeBadge(it) }
            }
            // Tek Text: kapalıyken 1 satır, açılınca tamamı. Yükseklik animateContentSize ile akar.
            item.weight.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(250, delayMillis = 80)) + expandVertically(tween(300)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(250)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.End),
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                    FilledTonalButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                }
            }
        }
    }
}

/** Fark rozeti: verilen kilo yeşil, alınan kırmızı, aynı kalan nötr (tasarim.md). */
@Composable
private fun ChangeBadge(change: Double) {
    val colors = LocalChangeColors.current
    val (container, content) = when {
        change < 0 -> colors.decreaseContainer to colors.onDecreaseContainer
        change > 0 -> colors.increaseContainer to colors.onIncreaseContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Badge(stringResource(R.string.change_kg, change), container, content)
}

/** Hap biçimli küçük etiket. İç dolgu 10/2 dp rozetin kendi ölçüsü (tasarim.md istisnası). */
@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ScaleIllustration()
        Spacer(Modifier.height(Spacing.xl))
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
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
        drawRoundRect(body, size = size, cornerRadius = CornerRadius(36.dp.toPx()))
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
