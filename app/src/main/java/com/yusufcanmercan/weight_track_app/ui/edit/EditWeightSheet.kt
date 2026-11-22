package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.home.HomeUiState
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.LocalSeyirColors
import com.yusufcanmercan.weight_track_app.util.fromPickerMillis
import com.yusufcanmercan.weight_track_app.util.startOfDay
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightSheet(
    viewModel: EditWeightViewModel,
    resultId: Long?,
    homeState: HomeUiState,
    onDismiss: () -> Unit,
    onResultDismiss: () -> Unit,
    onSaved: (Long?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showingResult = resultId != null
    val canHide by rememberUpdatedState(!state.busy)
    val sheet = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || canHide },
    )
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val interactive = !state.busy && !state.saved && !showingResult
    val canDismiss = (interactive || showingResult) && !closing
    val resultDismissCallback by rememberUpdatedState(onResultDismiss)
    val savedCallback by rememberUpdatedState(onSaved)

    fun closeResult() {
        if (closing) return
        closing = true
        scope.launch {
            sheet.hide()
            resultDismissCallback()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (canDismiss) {
                if (showingResult) onResultDismiss() else onDismiss()
            }
        },
        sheetState = sheet,
        sheetGesturesEnabled = canDismiss,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = canDismiss,
            shouldDismissOnClickOutside = canDismiss,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim,
    ) {
        val focus = LocalFocusManager.current
        LaunchedEffect(state.saved, resultId) {
            if (state.saved && resultId == null) {
                focus.clearFocus(force = true)
                savedCallback(state.savedEntryId)
            }
        }

        val colors = MaterialTheme.colorScheme
        val locale = LocalLocale.current.platformLocale
        val savedWeight = resultId?.let { id -> homeState.items.firstOrNull { it.weight.id == id }?.weight }
        val weightText = if (showingResult && !state.saved) {
            savedWeight?.let { String.format(locale, "%.1f", it.weight) }.orEmpty()
        } else {
            state.weightText
        }
        var confirmDelete by remember { mutableStateOf(false) }
        val weightRequester = remember { BringIntoViewRequester() }
        var revealWeightError by remember { mutableIntStateOf(0) }
        LaunchedEffect(revealWeightError, state.weightError) {
            if (revealWeightError > 0 && state.weightError) weightRequester.bringIntoView()
        }
        Column(
            Modifier.fillMaxWidth().imePadding()
                .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (showingResult) {
                        Box(
                            Modifier.size(32.dp).background(colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            SeyirIcon(R.drawable.ic_seyir_check, size = 20.dp, tint = colors.onPrimaryContainer)
                        }
                    }
                    val title = when {
                        showingResult -> R.string.feedback_title
                        state.isEditing -> R.string.edit_title
                        else -> R.string.add_title
                    }
                    Text(
                        stringResource(title),
                        Modifier.weight(1f).semantics {
                            heading()
                            if (showingResult) liveRegion = LiveRegionMode.Polite
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (state.isEditing) {
                        IconButton(onClick = { confirmDelete = true }, enabled = interactive) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = colors.error)
                        }
                    }
                    IconButton(
                        onClick = { if (showingResult) closeResult() else onDismiss() },
                        enabled = canDismiss,
                    ) {
                        val label = if (showingResult) R.string.back_to_today else R.string.cancel
                        SeyirIcon(R.drawable.ic_seyir_close, description = stringResource(label))
                    }
                }
                val weightError = if (state.weightError) stringResource(R.string.weight_error) else null
                Column(Modifier.fillMaxWidth().bringIntoViewRequester(weightRequester)) {
                    EntryWeight(
                        weightText,
                        isResult = showingResult,
                        enabled = interactive,
                        errorMessage = weightError,
                        onChange = viewModel::onWeightChange,
                    )
                    if (weightError != null) {
                        Text(
                            weightError,
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                AnimatedContent(
                    targetState = showingResult,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        (fadeIn(tween(140, delayMillis = 60)) togetherWith fadeOut(tween(60)))
                            .using(SizeTransform { _, _ -> tween(200) })
                    },
                    label = "Weight entry content",
                ) { result ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (result) {
                            WeightEntryResult(homeState, savedWeight)
                        } else {
                            EntryFormFields(viewModel, state, interactive, confirmDelete, { confirmDelete = it })
                        }
                    }
                }
            }
            if (!showingResult && (!state.available || state.writeError)) {
                Text(
                    stringResource(if (!state.available) R.string.entry_unavailable else R.string.entry_write_error),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.error,
                )
            }
            Button(
                onClick = {
                    if (showingResult) {
                        closeResult()
                    } else {
                        viewModel.onSave()
                        if (viewModel.uiState.value.weightError) revealWeightError++
                    }
                },
                enabled = if (showingResult) !closing else interactive && state.available,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (showingResult) R.string.back_to_today else R.string.save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EntryWeight(
    weightText: String,
    isResult: Boolean,
    enabled: Boolean,
    errorMessage: String?,
    onChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val numberStyle = MaterialTheme.typography.displayMedium.copy(
        color = if (errorMessage != null) colors.error else colors.onSurface,
        textAlign = TextAlign.Center,
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
    ) {
        if (isResult) {
            Text(weightText.ifEmpty { "–" }, Modifier.alignByBaseline(), style = numberStyle)
        } else {
            val numberText = rememberTextMeasurer().measure(
                weightText.ifEmpty { "–" }, numberStyle, softWrap = false, maxLines = 1,
            )
            val editorWidth = with(LocalDensity.current) { (numberText.size.width.toDp() + 4.dp).coerceIn(44.dp, 208.dp) }
            BasicTextField(
                value = weightText,
                onValueChange = onChange,
                modifier = Modifier.width(editorWidth).alignByBaseline().testTag(WEIGHT_INPUT_TAG).semantics {
                    if (errorMessage != null) error(errorMessage)
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = numberStyle,
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { input ->
                    Box(contentAlignment = Alignment.Center) {
                        if (weightText.isEmpty()) {
                            Text("–", style = MaterialTheme.typography.displayMedium, color = colors.onSurfaceVariant)
                        }
                        input()
                    }
                },
            )
        }
        Text("kg", Modifier.alignByBaseline(), style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryFormFields(
    viewModel: EditWeightViewModel,
    state: EditUiState,
    interactive: Boolean,
    confirmDelete: Boolean,
    onConfirmDeleteChange: (Boolean) -> Unit,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val locale = LocalLocale.current.platformLocale
    val timeFormat = android.text.format.DateFormat.getTimeFormat(LocalContext.current)
    val colors = MaterialTheme.colorScheme
    val value = state.weightText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    WeightRuler(value ?: 70.0, interactive) {
        viewModel.onRulerChange(it)
    }
    Text(
        stringResource(R.string.ruler_help),
        Modifier.fillMaxWidth(),
        color = colors.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
    HorizontalDivider(color = colors.outlineVariant)
    val measured = Date(state.dateMillis)
    val now = Calendar.getInstance()
    val today = startOfDay(now.timeInMillis)
    val year = now[Calendar.YEAR]
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = startOfDay(now.timeInMillis)
    val measurementDay = startOfDay(state.dateMillis)
    val dateLabel = when (measurementDay) {
        today -> stringResource(R.string.nav_home)
        yesterday -> stringResource(R.string.measurement_yesterday)
        else -> {
            val measurementYear = Calendar.getInstance().apply { time = measured }[Calendar.YEAR]
            val skeleton = if (measurementYear == year) "MMMd" else "yMMMd"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            SimpleDateFormat(pattern, locale).format(measured)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        MeasurementControl(
            stringResource(R.string.measurement_date), dateLabel, R.drawable.ic_seyir_history,
            enabled = interactive, onClick = { showDate = true }, modifier = Modifier.weight(1f),
            emphasized = measurementDay != today,
        )
        MeasurementControl(
            stringResource(R.string.measurement_time_title),
            if (state.hasMeasurementTime) timeFormat.format(measured) else stringResource(R.string.empty_value),
            R.drawable.ic_seyir_clock,
            enabled = interactive, onClick = { showTime = true }, modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(color = colors.outlineVariant)
    Text(stringResource(R.string.entry_mood), style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        EntryMood.entries.forEach { mood ->
            val selected = state.mood == mood
            Surface(
                selected = selected,
                onClick = { viewModel.onMoodChange(mood) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                enabled = interactive,
                shape = CircleShape,
                color = if (selected) colors.primary else colors.surfaceContainer,
                contentColor = when {
                    !state.busy && selected -> colors.onPrimary
                    !state.busy -> colors.onSurface
                    else -> colors.onSurfaceVariant
                },
                border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
            ) {
                Row(
                    Modifier.heightIn(min = 36.dp).padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SeyirIcon(mood.iconRes, size = 16.dp)
                    Text(stringResource(mood.labelRes), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }
    HorizontalDivider(color = colors.outlineVariant)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val label = stringResource(R.string.note_add)
        val hint = stringResource(R.string.note_hint)
        SeyirIcon(R.drawable.ic_seyir_note, size = 16.dp)
        Text(label, style = MaterialTheme.typography.bodySmall)
        BasicTextField(
            value = state.noteText,
            onValueChange = viewModel::onNoteChange,
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            maxLines = 3,
            enabled = interactive,
            decorationBox = { input ->
                Box {
                    if (state.noteText.isEmpty()) Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    input()
                }
            },
        )
    }
    HorizontalDivider(color = colors.outlineVariant)
    if (showDate) {
        val todayPicker = remember { toPickerMillis(System.currentTimeMillis()) }
        val date = rememberDatePickerState(
            initialSelectedDateMillis = toPickerMillis(state.dateMillis),
            yearRange = DatePickerDefaults.YearRange.first..Calendar.getInstance()[Calendar.YEAR],
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayPicker
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    date.selectedDateMillis?.let { viewModel.onDateChange(fromPickerMillis(it)) }
                    showDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(date) }
    }
    if (showTime) {
        val calendar = remember { Calendar.getInstance().apply { timeInMillis = state.dateMillis } }
        val time = rememberTimePickerState(calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.measurement_time_title)) },
            text = { TimeInput(time) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChange(time.hour, time.minute)
                    showTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { onConfirmDeleteChange(false) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { onConfirmDeleteChange(false); viewModel.onDelete() }) {
                    Text(stringResource(R.string.delete), color = colors.error)
                }
            },
            dismissButton = { TextButton(onClick = { onConfirmDeleteChange(false) }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

internal val EntryMood.labelRes: Int
    get() = when (this) {
        EntryMood.GOOD -> R.string.mood_good
        EntryMood.CALM -> R.string.mood_calm
        EntryMood.TIRED -> R.string.mood_tired
    }

internal val EntryMood.iconRes: Int
    get() = when (this) {
        EntryMood.GOOD -> R.drawable.ic_seyir_good
        EntryMood.CALM -> R.drawable.ic_seyir_calm
        EntryMood.TIRED -> R.drawable.ic_seyir_tired
    }

@Composable
private fun MeasurementControl(
    label: String,
    value: String,
    @DrawableRes icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { role = Role.Button },
        shape = MaterialTheme.shapes.small,
        color = if (emphasized) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (emphasized) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SeyirIcon(icon, size = 16.dp)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun WeightRuler(value: Double, enabled: Boolean, onChange: (Double) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val minorTick = LocalSeyirColors.current.chartDots
    val shape = MaterialTheme.shapes.small
    val selectedTick by rememberUpdatedState((value.coerceIn(1.0, 500.0) * 10).roundToInt())
    val update by rememberUpdatedState(onChange)
    val label = stringResource(R.string.ruler_label)
    val measurer = rememberTextMeasurer()
    val tickText = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val tickSpacing = 10.dp
    val scope = rememberCoroutineScope()
    var visualTick by remember { mutableStateOf<Float?>(null) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun select(tick: Int) = update(tick.coerceIn(10, 5000) / 10.0)

    fun settle(from: Float, to: Int) {
        settleJob?.cancel()
        select(to)
        visualTick = from
        settleJob = scope.launch {
            animate(from, to.toFloat(), animationSpec = tween(140)) { tick, _ -> visualTick = tick }
            visualTick = null
        }
    }

    Canvas(
        Modifier.fillMaxWidth().height(74.dp)
            .clip(shape)
            .background(colors.surfaceContainer, shape).border(1.dp, colors.outlineVariant, shape)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(1.0, 500.0).toFloat(), 1f..500f)
                if (enabled) setProgress { update(it.toDouble()); true }
            }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures {
                    val from = visualTick ?: selectedTick.toFloat()
                    val tapped = from + (it.x - size.width / 2f) / tickSpacing.toPx()
                    settle(from, tapped.roundToInt().coerceIn(10, 5000))
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    var tick = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            settleJob?.cancel()
                            tick = visualTick ?: selectedTick.toFloat()
                        },
                        onDragEnd = { settle(tick, tick.roundToInt()) },
                        onDragCancel = { settle(tick, tick.roundToInt()) },
                    ) { change, width ->
                        change.consume()
                        tick = (tick - width / tickSpacing.toPx()).coerceIn(10f, 5000f)
                        visualTick = tick
                        select(tick.roundToInt())
                    }
                }
            },
    ) {
        val step = tickSpacing.toPx()
        val middle = visualTick ?: selectedTick.toFloat()
        val nearestTick = middle.roundToInt()
        val sideTicks = ceil(size.width / (2 * step)).toInt()
        for (tenth in (nearestTick - sideTicks)..(nearestTick + sideTicks)) {
            if (tenth !in 10..5000) continue
            val x = center.x + (tenth - middle) * step
            val whole = tenth % 10 == 0
            val half = tenth % 5 == 0
            val bottom = if (whole) 43.dp else if (half) 37.dp else 30.dp
            drawLine(
                color = if (whole) colors.onSurfaceVariant else minorTick,
                start = Offset(x, 16.dp.toPx()),
                end = Offset(x, bottom.toPx()),
                strokeWidth = if (whole) 1.4.dp.toPx() else 1.dp.toPx(),
            )
            if (whole) {
                val tick = measurer.measure((tenth / 10).toString(), tickText)
                val left = x - tick.size.width / 2f
                if (left >= 0f && left + tick.size.width <= size.width) {
                    drawText(tick, topLeft = Offset(left, 52.dp.toPx()))
                }
            }
        }
        drawLine(colors.secondary, Offset(center.x, 8.dp.toPx()), Offset(center.x, size.height - 12.dp.toPx()), 2.dp.toPx())
        drawCircle(colors.secondary, radius = 4.dp.toPx(), center = Offset(center.x, 8.dp.toPx()))
    }
}

/** UI testlerinin kilo alanını bulduğu etiket; alanın görünür bir başlığı yok. */
internal const val WEIGHT_INPUT_TAG = "weight_input"
