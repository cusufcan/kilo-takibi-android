package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.util.fromPickerMillis
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.text.DateFormat
import java.util.Date

/** ViewModel'i bağlayan ince kabuk. Kaydedilince `onSaved`, geri okunda `onBack` ile kapanır. */
@Composable
fun EditWeightScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditWeightViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    EditWeightContent(
        state = state,
        onWeightChange = viewModel::onWeightChange,
        onNoteChange = viewModel::onNoteChange,
        onDateChange = viewModel::onDateChange,
        onSave = viewModel::onSave,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightContent(
    state: EditUiState,
    onWeightChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = stringResource(if (state.isEditing) R.string.edit_title else R.string.add_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        WeightValueBox(state.weightText, state.weightError, onWeightChange)
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                DateRow(state.dateMillis, onClick = { showDatePicker = true })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NoteRow(state.noteText, onNoteChange)
            }
        }
        Spacer(Modifier.weight(1f))
        // Ekrandaki tek ana buton: tam genişlik, hap, mor (tasarim.md).
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
}

/** Büyük değer kutusu: ortada kocaman kilo, sonunda " kg" eki. */
@Composable
private fun WeightValueBox(text: String, isError: Boolean, onChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.weight_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = text,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = KgSuffix,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    // Boşken kutu çıplak görünmesin: soluk örnek değer.
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.weight_kg, 0.0),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        field()
                    }
                },
            )
            if (isError) {
                Text(
                    text = stringResource(R.string.weight_error),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
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

/** Grup satırı: solda ikon, ortada içerik, `lg`/`md` dolgu. */
@Composable
private fun GroupRow(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        icon()
        content()
    }
}

@Composable
private fun DateRow(dateMillis: Long, onClick: () -> Unit) {
    GroupRow(
        icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = stringResource(R.string.edit_date),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = DateFormat.getDateInstance().format(Date(dateMillis)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteRow(text: String, onChange: (String) -> Unit) {
    GroupRow(
        icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
