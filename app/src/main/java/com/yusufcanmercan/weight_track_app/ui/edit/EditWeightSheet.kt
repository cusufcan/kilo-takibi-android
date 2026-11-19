package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel.Companion.STEP_KG
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.util.fromPickerMillis
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Ekle/düzenle alt sayfası (ADR-0015). ViewModel'i açan ekran verir ve `start(id)` ile kurar;
 * kaydedilince/silinince sayfa kayarak kapanır, sonra `onDismiss` çağrılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightSheet(viewModel: EditWeightViewModel, onDismiss: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Yarım açık durak yok: sayfa ya tam açık ya kapalı.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }

    LaunchedEffect(state.saved) {
        if (state.saved) close()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        EditWeightContent(
            state = state,
            onWeightChange = viewModel::onWeightChange,
            onStep = viewModel::onStep,
            onNoteChange = viewModel::onNoteChange,
            onDateChange = viewModel::onDateChange,
            onSave = viewModel::onSave,
            onDelete = viewModel::onDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightContent(
    state: EditUiState,
    onWeightChange: (String) -> Unit,
    onStep: (Double) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Not alanı istenince açılır; düzenlenen kayıtta not varsa baştan açık.
    var noteOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (state.isEditing) R.string.edit_title else R.string.add_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.isEditing) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        WeightStepper(state.weightText, state.weightError, onWeightChange, onStep)

        // Tarih ve not: tek satırda iki küçük düğme; ana dikkat kiloda kalsın.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(DateFormat.getDateInstance().format(Date(state.dateMillis))) },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            AssistChip(
                onClick = { noteOpen = true },
                label = {
                    Text(
                        text = state.noteText.ifEmpty { stringResource(R.string.note_add) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (noteOpen || state.noteText.isNotEmpty()) {
            NoteField(state.noteText, onNoteChange)
        }

        // Tek ana buton: tam genişlik, hap, mor (tasarim.md).
        Button(
            onClick = onSave,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = toPickerMillis(state.dateMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChange(fromPickerMillis(it)) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    // Silme geri alınamaz: önce sor.
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Ortada büyük kilo (elle de yazılabilir), iki yanında 0,1 kg'lık −/+ düğmeleri. */
@Composable
private fun WeightStepper(
    text: String,
    isError: Boolean,
    onChange: (String) -> Unit,
    onStep: (Double) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val stepDown = stringResource(R.string.step_down)
            StepButton(onClick = { onStep(-STEP_KG) }) {
                // Çekirdek ikon setinde eksi yok; metin eksi işareti, ekran okuyucuya "Azalt".
                Text(
                    "−",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { contentDescription = stepDown },
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = KgSuffix,
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    // Boşken (ilk kayıt) soluk örnek değer.
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.weight_kg, 0.0),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        field()
                    }
                },
            )
            StepButton(onClick = { onStep(STEP_KG) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.step_up))
            }
        }
        if (isError) {
            Text(
                text = stringResource(R.string.weight_error),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun StepButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp), content = content)
}

/** Not alanı: açık gri yüzey içinde çıplak metin alanı, altta karakter sayacı. */
@Composable
private fun NoteField(text: String, onChange: (String) -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
            BasicTextField(
                value = text,
                onValueChange = onChange,
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.note_label),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        field()
                    }
                },
            )
            Text(
                text = "${text.length}/${EditWeightViewModel.MAX_NOTE_LENGTH}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = Spacing.xs),
            )
        }
    }
}

/**
 * Metnin sonuna yalnız görüntüde " kg" ekler; kayıtlı değer değişmez.
 * İmleç eke giremez: görüntüdeki konum metin uzunluğunu aşarsa sona sabitlenir.
 */
private object KgSuffix : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val length = text.length
        return TransformedText(
            AnnotatedString(text.text + " kg"),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(length)
            },
        )
    }
}
