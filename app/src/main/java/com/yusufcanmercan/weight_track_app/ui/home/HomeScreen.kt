package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightSheet
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel
import com.yusufcanmercan.weight_track_app.ui.theme.LocalChangeColors
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** ViewModel'i bağlayan ince kabuk. Çizimi `HomeContent` yapar. */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    editViewModel: EditWeightViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Sayfa açık mı ve hangi kayıt: döndürmede ve süreç ölümünde korunur (ADR-0015).
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    val open: (Long?) -> Unit = { id ->
        editId = id
        editViewModel.start(id)
        sheetOpen = true
    }
    // Süreç ölüp geri gelince sayfa açık ama ViewModel yeni: aynı kaydı yeniden yükle.
    LaunchedEffect(sheetOpen) {
        if (sheetOpen && !editViewModel.isStarted) editViewModel.start(editId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(state = state, onEdit = { open(it) }, onOpenSettings = onOpenSettings)
        FloatingActionButton(
            onClick = { open(null) },
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
    if (sheetOpen) {
        EditWeightSheet(viewModel = editViewModel, onDismiss = { sheetOpen = false })
    }
}

/** Son satır FAB'ın altında kalmasın: FAB (56) + iki yanındaki `lg` boşluk. */
private val FabClearance = 56.dp + Spacing.lg * 2

@Composable
fun HomeContent(
    state: HomeUiState,
    onEdit: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoading && state.items.isEmpty()) {
        EmptyState(modifier = modifier.fillMaxSize())
        return
    }
    // Seçili aralık ekran durumu: döndürmede korunur, ViewModel'e gerek yok.
    var range by rememberSaveable { mutableStateOf(ChartRange.MONTH) }
    val chart = remember(state.items, range, state.goal) {
        val filtered = filterByRange(state.items.map { it.weight }, range)
        if (filtered.isEmpty()) null else buildChartData(filtered, state.goal?.goalKg)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = FabClearance),
    ) {
        item(key = "title") {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
        }
        state.latest?.let { latest ->
            item(key = "summary") {
                Summary(latest, state, onOpenSettings, Modifier.padding(top = Spacing.xl))
            }
        }
        item(key = "range") {
            RangeSelector(range, onSelect = { range = it }, Modifier.padding(top = Spacing.xl, bottom = Spacing.md))
        }
        chart?.let { data ->
            item(key = "chart") { WeightChart(data) }
        }
        state.months.forEach { month ->
            item(key = "month-${month.year}-${month.month}") {
                MonthHeader(month, Modifier.padding(top = Spacing.xl, bottom = Spacing.xs))
            }
            itemsIndexed(month.items, key = { _, item -> item.weight.id }) { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                WeightRow(item, onClick = { onEdit(item.weight.id) })
            }
        }
    }
}

/** Büyük güncel kilo + haftalık rozet, altında BMI ve hedef tek düz satır (tasarim.md v2). */
@Composable
private fun Summary(latest: Weight, state: HomeUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f, fill = false)) {
                Text("%.1f".format(latest.weight), style = MaterialTheme.typography.displayLarge)
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.sm),
                )
            }
            state.weeklyChange?.let {
                Spacer(Modifier.width(Spacing.md))
                ChangeBadge(it, R.string.home_week_change)
            }
        }
        if (state.weeklyChange == null) {
            Text(
                text = stringResource(R.string.home_week_no_data),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InfoLine(state, onOpenSettings, Modifier.padding(top = Spacing.sm))
    }
}

/** "BMI 24,1 · Normal · 5,0 kg kaldı". Eksik ayar varsa o parça ayarlara götüren bağlantıdır. */
@Composable
private fun InfoLine(state: HomeUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val bmi = state.bmi
    val goal = state.goal
    val bmiText = when {
        bmi != null -> "BMI %.1f · %s".format(bmi.value, stringResource(bmiCategoryText(bmi.category)))
        !state.hasHeight -> null
        else -> ""
    }
    val goalText = when {
        goal == null -> null
        !goal.reached -> stringResource(R.string.home_goal_remaining, abs(goal.remaining))
        // Hedefin 0,1 kg'dan fazla ötesindeysek ne kadar aştığını da söyle.
        abs(goal.remaining) < 0.1 -> stringResource(R.string.home_goal_reached)
        goal.remaining < 0 -> stringResource(R.string.home_goal_reached) + " · " +
            stringResource(R.string.home_goal_reached_below, abs(goal.remaining))
        else -> stringResource(R.string.home_goal_reached) + " · " +
            stringResource(R.string.home_goal_reached_above, goal.remaining)
    }
    Column(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (bmiText == null) LinkText(stringResource(R.string.home_bmi_no_height), onOpenSettings)
            else if (bmiText.isNotEmpty()) InfoText(bmiText)
            InfoText("·")
            if (goalText == null) LinkText(stringResource(R.string.home_goal_none), onOpenSettings)
            else InfoText(goalText)
        }
        if (bmi != null) {
            Text(
                text = stringResource(R.string.home_bmi_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

private fun bmiCategoryText(category: BmiCategory) = when (category) {
    BmiCategory.UNDERWEIGHT -> R.string.bmi_underweight
    BmiCategory.NORMAL -> R.string.bmi_normal
    BmiCategory.OVERWEIGHT -> R.string.bmi_overweight
    BmiCategory.OBESE -> R.string.bmi_obese
}

@Composable
private fun InfoText(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun LinkText(text: String, onClick: () -> Unit) = Text(
    text = "$text ›",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.clickable(onClick = onClick),
)

/** 7G / 30G / 90G / Tümü. Seçili olan dolu mor hap, diğerleri düz yazı. */
@Composable
private fun RangeSelector(selected: ChartRange, onSelect: (ChartRange) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ChartRange.entries.forEach { range ->
            val isSelected = range == selected
            Surface(
                onClick = { onSelect(range) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(
                        when (range) {
                            ChartRange.WEEK -> R.string.range_week
                            ChartRange.MONTH -> R.string.range_month
                            ChartRange.QUARTER -> R.string.range_quarter
                            ChartRange.ALL -> R.string.range_all
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: MonthGroup, modifier: Modifier = Modifier) {
    val text = remember(month.year, month.month) {
        val calendar = Calendar.getInstance().apply { clear(); set(month.year, month.month, 1) }
        // "LLLL": ayın tek başına (yalın) adı, ör. "Kasım"; Türkçede küçük harfle gelir, ilk harf büyütülür.
        SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(calendar.time)
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Kartsız satır: dokununca düzenleme açılır (silme de orada). */
@Composable
private fun WeightRow(item: HomeItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.weight_kg, item.weight.weight), style = MaterialTheme.typography.bodyLarge)
            // Gün bazında gösterim: saat kısmı anlamsız.
            val date = DateFormat.getDateInstance().format(Date(item.weight.timeStamp))
            val note = item.weight.note
            Text(
                text = if (note.isNullOrBlank()) date else "$date · $note",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item.change?.let {
            Spacer(Modifier.width(Spacing.md))
            ChangeBadge(it, R.string.change_kg)
        }
    }
}

/** Fark rozeti: verilen kilo yeşil, alınan kırmızı, aynı kalan nötr (tasarim.md). */
@Composable
private fun ChangeBadge(change: Double, textRes: Int) {
    val colors = LocalChangeColors.current
    val (container, content) = when {
        change < 0 -> colors.decreaseContainer to colors.onDecreaseContainer
        change > 0 -> colors.increaseContainer to colors.onIncreaseContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        // İç dolgu 10/2 dp rozetin kendi ölçüsü (tasarim.md istisnası).
        Text(
            text = stringResource(textRes, change),
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
