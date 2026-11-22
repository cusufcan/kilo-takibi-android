package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.hasKnownMeasurementTime
import com.yusufcanmercan.weight_track_app.ui.edit.EntryMood
import com.yusufcanmercan.weight_track_app.ui.edit.labelRes
import com.yusufcanmercan.weight_track_app.ui.edit.iconRes
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIconTile
import com.yusufcanmercan.weight_track_app.ui.theme.LocalSeyirColors
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val HistoryHeaderItems = 2
private val HistoryRowHeight = 64.dp

@Composable
fun HistoryScreen(state: HomeUiState, onEdit: (Long) -> Unit, onAdd: () -> Unit) {
    val today = rememberToday()
    var selectedMonth by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val latestMonth = firstOfMonth(maxOf(today, state.dailyWeights.lastOrNull()?.dayMillis ?: today))
    val month = selectedMonth ?: latestMonth
    val firstMonth = firstOfMonth(state.dailyWeights.firstOrNull()?.dayMillis ?: today)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val motionScale = scope.coroutineContext[MotionDurationScale]
    val rowHeight = with(LocalDensity.current) { HistoryRowHeight.roundToPx() }
    var revealJob by remember { mutableStateOf<Job?>(null) }
    val next = nextMonth(month, 1)
    val rows = remember(month, state.months) {
        val calendar = Calendar.getInstance(Utc).apply { timeInMillis = month }
        val year = calendar[Calendar.YEAR]
        val monthIndex = calendar[Calendar.MONTH]
        state.months.firstOrNull { it.year == year && it.month == monthIndex }?.items.orEmpty()
    }
    val days = remember(month, next, state.dailyWeights) {
        state.dailyWeights.filter { it.dayMillis in month until next }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.lg)) {
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.xl, bottom = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle(stringResource(R.string.nav_history))
            if (state.items.isNotEmpty()) {
                TextButton(onClick = onAdd) { Text(stringResource(R.string.home_add_entry)) }
            }
        }
        if (state.isLoading) {
            StatNote(stringResource(R.string.loading))
            return@Column
        }
        if (state.items.isEmpty()) {
            FirstEntryCard(R.drawable.ic_seyir_history, stringResource(R.string.history_first_entry_hint), onAdd)
            return@Column
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = Spacing.lg)) {
            item(key = "calendar") {
                MonthCalendar(
                    month, next, days, selectedDay ?: days.lastOrNull()?.dayMillis,
                    canGoBack = month > firstMonth,
                    canGoForward = month < latestMonth,
                    onMove = {
                        revealJob?.cancel()
                        selectedMonth = nextMonth(month, it)
                        selectedDay = null
                    },
                    onSelect = { day ->
                        selectedDay = day
                        revealJob?.cancel()
                        val index = rows.indexOfFirst { toPickerMillis(it.weight.timeStamp) == day }
                        if (index >= 0) {
                            revealJob = scope.launch { listState.revealHistoryRow(index + HistoryHeaderItems, rowHeight) }
                        }
                    },
                    modifier = Modifier.graphicsLayer {
                        val calendar = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "calendar" }
                        val collapse = if (calendar == null || motionScale?.scaleFactor == 0f) 0f else {
                            (-calendar.offset / calendar.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        }
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleY = 1f - collapse * 0.14f
                        alpha = 1f - collapse * 0.4f
                    },
                )
            }
            stickyHeader(key = "month") {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(monthLabel(month), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        StatNote(pluralStringResource(R.plurals.history_entries, rows.size, rows.size))
                    }
                }
            }
            if (rows.isEmpty()) item {
                Card {
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = {
                        selectedMonth = state.dailyWeights.lastOrNull()?.let { firstOfMonth(it.dayMillis) }
                        selectedDay = null
                    }) { Text(stringResource(R.string.history_latest_month)) }
                }
            }
            itemsIndexed(rows, key = { _, row -> row.weight.id }) { index, row ->
                val shape = MaterialTheme.shapes.large
                val square = CornerSize(0.dp)
                val rowShape = shape.copy(
                    topStart = if (index == 0) shape.topStart else square,
                    topEnd = if (index == 0) shape.topEnd else square,
                    bottomStart = if (index == rows.lastIndex) shape.bottomStart else square,
                    bottomEnd = if (index == rows.lastIndex) shape.bottomEnd else square,
                )
                Surface(
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(130),
                        placementSpec = tween(180),
                        fadeOutSpec = tween(150),
                    ),
                    shape = rowShape,
                    color = if ((selectedDay ?: days.lastOrNull()?.dayMillis) == toPickerMillis(row.weight.timeStamp)) {
                        LocalSeyirColors.current.heatLow
                    } else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column {
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        HistoryRow(row, onEdit)
                    }
                }
            }
        }
    }
}

private suspend fun LazyListState.revealHistoryRow(index: Int, minimumRowHeight: Int) {
    var layout = layoutInfo
    if (layout.visibleItemsInfo.none { it.index == index }) {
        val viewportHeight = layout.viewportEndOffset - layout.viewportStartOffset
        val rowHeight = layout.visibleItemsInfo.firstOrNull { it.index >= HistoryHeaderItems }?.size ?: minimumRowHeight
        val revealOffset = if (index > firstVisibleItemIndex) (viewportHeight - rowHeight).coerceAtLeast(0) else 0
        animateScrollToItem(index, scrollOffset = -revealOffset)
        layout = layoutInfo
    }
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val pinnedHeader = layout.visibleItemsInfo.firstOrNull {
        it.key == "month" && it.offset <= layout.viewportStartOffset
    }
    val visibleTop = pinnedHeader?.let { it.offset + it.size } ?: layout.viewportStartOffset
    val distance = when {
        item.offset < visibleTop -> item.offset - visibleTop
        item.offset + item.size > layout.viewportEndOffset -> item.offset + item.size - layout.viewportEndOffset
        else -> 0
    }
    if (distance != 0) animateScrollBy(distance.toFloat(), tween(180))
}

@Composable
private fun monthLabel(month: Long): String {
    val locale = LocalLocale.current.platformLocale
    return remember(month, locale) {
        SimpleDateFormat("LLLL yyyy", locale).apply { timeZone = Utc }.format(Date(month))
            .replaceFirstChar { it.titlecase(locale) }
    }
}

@Composable
private fun MonthCalendar(
    month: Long,
    next: Long,
    days: List<DailyWeight>,
    selected: Long?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onMove: (Int) -> Unit,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val colors = MaterialTheme.colorScheme
    val palette = LocalSeyirColors.current
    val dates = remember(month, next, locale) {
        val calendar = Calendar.getInstance(Utc, locale).apply { timeInMillis = month }
        val offset = (calendar[Calendar.DAY_OF_WEEK] - calendar.firstDayOfWeek + 7) % 7
        val labels = (0..6).map { index ->
            val day = ((calendar.firstDayOfWeek - 1 + index) % 7) + 1
            java.text.DateFormatSymbols.getInstance(locale).shortWeekdays[day]
        }
        val count = ((next - month) / DAY_MILLIS).toInt()
        labels to (List<Long?>(offset) { null } + (0 until count).map { month + it * DAY_MILLIS })
    }
    val recorded = remember(days) { days.associateBy { it.dayMillis } }
    val min = days.minOfOrNull { it.measuredKg } ?: 0.0
    val max = days.maxOfOrNull { it.measuredKg } ?: min
    Card(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMove(-1) }, enabled = canGoBack) {
                SeyirIcon(R.drawable.ic_seyir_prev, description = stringResource(R.string.month_previous), size = 16.dp)
            }
            Text(monthLabel(month), fontWeight = FontWeight.Bold)
            IconButton(onClick = { onMove(1) }, enabled = canGoForward) {
                SeyirIcon(R.drawable.ic_seyir_next, description = stringResource(R.string.month_next), size = 16.dp)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            dates.first.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { StatNote(day) }
            }
        }
        dates.second.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                repeat(7) { index ->
                    val date = week.getOrNull(index)
                    val entry = recorded[date]
                    if (date == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val isSelected = date == selected
                        val heat = if (entry == null || max == min) 0f else ((entry.measuredKg - min) / (max - min)).toFloat()
                        val fill = when {
                            isSelected -> palette.heatSelected
                            entry != null -> lerp(palette.heatLow, palette.heatHigh, heat)
                            else -> Color.Transparent
                        }
                        val label = formattedDay(date, DateFormat.LONG)
                        val description = if (entry != null) stringResource(R.string.history_calendar_entry, label) else label
                        val button = Modifier.weight(1f).heightIn(min = 32.dp).clip(MaterialTheme.shapes.small).background(fill)
                        Box(
                            button.then(if (entry == null) Modifier else Modifier.clickable { onSelect(date) })
                                .semantics {
                                    contentDescription = description
                                    if (entry != null) this.selected = isSelected
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val ink = if (isSelected) palette.onHeatSelected else colors.onSurface
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    ((date - month) / DAY_MILLIS + 1).toString(),
                                    color = ink,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clearAndSetSemantics {},
                                )
                                if (entry != null) Box(Modifier.size(4.dp).background(if (selected == date) ink else colors.secondary, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).background(colors.secondary, CircleShape))
            StatNote(stringResource(R.string.history_calendar_legend))
        }
    }
}

@Composable
private fun HistoryRow(row: HomeItem, onEdit: (Long) -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val timestamp = Date(row.weight.timeStamp)
    val time = android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(timestamp)
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMdEEEE")
    val date = SimpleDateFormat(pattern, locale).format(timestamp)
    val mood = EntryMood.entries.find { it.name == row.weight.mood }
    val details = listOfNotNull(time.takeIf { row.weight.hasKnownMeasurementTime() }, row.weight.note)
    Row(
        Modifier.fillMaxWidth().heightIn(min = HistoryRowHeight).clickable { onEdit(row.weight.id) }
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SeyirIconTile(
            mood?.iconRes ?: R.drawable.ic_seyir_history,
            description = mood?.let { stringResource(it.labelRes) },
            tileSize = 32.dp,
            iconSize = 16.dp,
        )
        Column(Modifier.weight(1f)) {
            HistoryChangingText(date, MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            HistoryChangingText(
                details.joinToString(" · "),
                MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
            )
        }
        HistoryChangingText(
            stringResource(R.string.weight_kg, row.weight.weight),
            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun HistoryChangingText(text: String, style: TextStyle, maxLines: Int = Int.MAX_VALUE) {
    Crossfade(targetState = text, animationSpec = tween(150), label = "Measurement field") { value ->
        if (value.isNotEmpty()) Text(value, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}
