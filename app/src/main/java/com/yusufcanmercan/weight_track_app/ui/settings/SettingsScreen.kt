package com.yusufcanmercan.weight_track_app.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.settings.DailyReminder
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.ui.home.ScreenTitle
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    hasMeasurements: Boolean,
    viewModel: SettingsViewModel = hiltViewModel(),
    profileContent: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fieldStatus by viewModel.fieldStatus.collectAsStateWithLifecycle()
    // Dilin tek kaynağı Android'in kendisi; ekran yalnız açılışta okur, seçince Activity zaten yenilenir.
    var language by remember { mutableStateOf(AppLanguage.current()) }

    SettingsContent(
        state = state,
        fieldStatus = fieldStatus,
        onFieldChanged = viewModel::clearFieldStatus,
        language = language,
        onThemeChange = viewModel::onThemeChange,
        onGoalSave = viewModel::onGoalSave,
        onHeightSave = viewModel::onHeightSave,
        onNameSave = viewModel::onNameSave,
        onReminderEnabledChange = viewModel::onReminderEnabledChange,
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onBack = onBack,
        onLanguageChange = {
            language = it
            AppLanguage.apply(it)
        },
        profileContent = {
            CsvExportSection(viewModel, hasMeasurements)
            profileContent()
        },
    )
}

@Composable
private fun CsvExportSection(viewModel: SettingsViewModel, hasMeasurements: Boolean) {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val status by viewModel.csvExportStatus.collectAsStateWithLifecycle()
    val filename = stringResource(R.string.settings_export_filename)
    var pickerUnavailable by rememberSaveable { mutableStateOf(false) }
    val contract = remember { ActivityResultContracts.CreateDocument("text/csv") }
    val saveDocument = rememberLauncherForActivityResult(contract) { uri ->
        if (uri != null) viewModel.exportCsv { resolver.openOutputStream(uri) }
    }
    val busy = status == CsvExportStatus.WRITING
    Section(R.string.settings_data) {
        Column(
            Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(if (hasMeasurements) R.string.settings_export_hint else R.string.settings_export_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                enabled = hasMeasurements && !busy,
                onClick = {
                    pickerUnavailable = false
                    viewModel.clearCsvExportStatus()
                    try {
                        saveDocument.launch(filename)
                    } catch (_: ActivityNotFoundException) {
                        pickerUnavailable = true
                    }
                },
            ) {
                Text(stringResource(R.string.settings_export_csv))
            }
            val message = when {
                pickerUnavailable || status == CsvExportStatus.FAILED -> R.string.settings_export_error
                busy -> R.string.settings_export_running
                status == CsvExportStatus.SAVED -> R.string.settings_export_saved
                else -> null
            }
            if (message != null) {
                Text(
                    stringResource(message),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message == R.string.settings_export_error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    fieldStatus: Map<SettingsField, FieldSaveStatus>,
    onFieldChanged: (SettingsField) -> Unit,
    language: AppLanguage,
    onThemeChange: (ThemeMode) -> Unit,
    onGoalSave: (String) -> Boolean,
    onHeightSave: (String) -> Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onNameSave: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
    profileContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                SeyirIcon(R.drawable.ic_seyir_back, description = stringResource(R.string.go_back))
            }
            ScreenTitle(stringResource(R.string.profile_settings))
        }
        Section(R.string.settings_goal_and_measurements) {
            NumberField(
                value = state.goalKg,
                status = fieldStatus[SettingsField.GOAL],
                loading = state.isLoading,
                onChanged = { onFieldChanged(SettingsField.GOAL) },
                labelRes = R.string.settings_goal_field,
                hintRes = R.string.settings_goal_hint,
                invalidRes = R.string.settings_goal_invalid,
                onSave = onGoalSave,
            )
            Divider()
            NumberField(
                value = state.heightCm,
                status = fieldStatus[SettingsField.HEIGHT],
                loading = state.isLoading,
                onChanged = { onFieldChanged(SettingsField.HEIGHT) },
                labelRes = R.string.settings_height_field,
                hintRes = R.string.settings_height_hint,
                invalidRes = R.string.settings_height_invalid,
                onSave = onHeightSave,
            )
        }
        Section(R.string.profile_name) {
            var name by rememberSaveable(state.displayName) { mutableStateOf(state.displayName.orEmpty()) }
            val status = fieldStatus[SettingsField.NAME]
            val focus = LocalFocusManager.current
            LaunchedEffect(status) {
                if (status == FieldSaveStatus.SAVED) focus.clearFocus()
            }
            Column(
                Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(SettingsViewModel.MAX_NAME_LENGTH)
                        onFieldChanged(SettingsField.NAME)
                    },
                    enabled = !state.isLoading && status != FieldSaveStatus.WRITING,
                    label = { Text(stringResource(R.string.profile_name)) },
                    supportingText = { FieldStatusText(status, R.string.profile_name_hint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                FilledTonalButton(
                    onClick = { onNameSave(name) },
                    enabled = !state.isLoading && status != FieldSaveStatus.WRITING && status != FieldSaveStatus.SAVED &&
                        name.trim() != state.displayName.orEmpty(),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
        ReminderSection(state.reminder, onReminderEnabledChange, onReminderTimeChange)
        Section(R.string.settings_theme) {
            ThemeMode.entries.forEachIndexed { i, mode ->
                if (i > 0) Divider()
                Option(
                    label = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        },
                    ),
                    selected = state.themeMode == mode,
                    onClick = { onThemeChange(mode) },
                )
            }
        }
        Section(R.string.settings_language) {
            AppLanguage.entries.forEachIndexed { i, lang ->
                if (i > 0) Divider()
                Option(
                    label = stringResource(
                        when (lang) {
                            AppLanguage.SYSTEM -> R.string.language_system
                            AppLanguage.TURKISH -> R.string.language_tr
                            AppLanguage.ENGLISH -> R.string.language_en
                        },
                    ),
                    selected = language == lang,
                    onClick = { onLanguageChange(lang) },
                )
            }
        }
        profileContent()
    }
}

/** Günlük tartı hatırlatıcısı: aç/kapa, saat ve bildirim engelliyse uyarı (ADR-0040). */
@Composable
private fun ReminderSection(
    reminder: DailyReminder,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val notifications = rememberNotificationAccess()
    var showTime by rememberSaveable { mutableStateOf(false) }
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeText = remember(reminder.minuteOfDay, is24Hour) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60)
            set(Calendar.MINUTE, reminder.minuteOfDay % 60)
        }
        android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
    }
    Section(R.string.settings_reminders) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(reminder.enabled, role = Role.Switch) { notifications.change(it, onEnabledChange) }
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.reminder_toggle), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = reminder.enabled, onCheckedChange = null)
        }
        if (reminder.enabled) {
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { showTime = true }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.reminder_time), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(timeText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (!notifications.allowed) NotificationsBlockedWarning(Modifier.padding(horizontal = Spacing.lg))
        }
        if (notifications.denied) {
            Text(
                stringResource(R.string.weekly_notification_denied),
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (showTime) {
        val time = rememberTimePickerState(reminder.minuteOfDay / 60, reminder.minuteOfDay % 60, is24Hour)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimeInput(time) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(time.hour, time.minute)
                    showTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Bölüm: üstte `titleMedium` başlık, altında nötr grup kutusu (tasarim.md). */
@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Sayı girişi (hedef kilo, boy). Boş kaydetmek değeri siler; geçersizse hata gösterir, kaydetmez. */
@Composable
private fun NumberField(
    value: Double?,
    status: FieldSaveStatus?,
    loading: Boolean,
    onChanged: () -> Unit,
    labelRes: Int,
    hintRes: Int,
    invalidRes: Int,
    onSave: (String) -> Boolean,
) {
    // Kayıtlı değer değişince (ör. ilk yükleme) metin ona eşitlenir.
    val decimalSeparator = DecimalFormatSymbols.getInstance(LocalLocale.current.platformLocale).decimalSeparator
    val formatted = value?.let { "%.1f".format(Locale.ROOT, it).replace('.', decimalSeparator) }.orEmpty()
    var text by rememberSaveable(value, decimalSeparator) { mutableStateOf(formatted) }
    val normalized = text.replace(decimalSeparator, '.')
    val isDirty = text != formatted && (text.isNotBlank() || value != null) &&
        (normalized.replace(',', '.').toDoubleOrNull() != value || (value == null && text.isNotBlank()))
    val enabled = !loading && status != FieldSaveStatus.WRITING
    val focus = LocalFocusManager.current
    LaunchedEffect(status) {
        if (status == FieldSaveStatus.SAVED) focus.clearFocus()
    }
    var error by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = false
                        onChanged()
                    },
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { field ->
                        Box {
                            if (text.isEmpty()) {
                                Text("—", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            field()
                        }
                    },
                )
            }
            // İkincil eylem: mor değil, tonlu (ekranda tek ana buton kuralı).
            FilledTonalButton(onClick = { error = !onSave(normalized) }, enabled = enabled && status != FieldSaveStatus.SAVED && isDirty) {
                Text(stringResource(R.string.settings_goal_save))
            }
        }
        FieldStatusText(status, if (error) invalidRes else hintRes, error)
    }
}

@Composable
private fun FieldStatusText(status: FieldSaveStatus?, hintRes: Int, validationError: Boolean = false) {
    val message = when (status) {
        FieldSaveStatus.WRITING -> R.string.settings_field_saving
        FieldSaveStatus.SAVED -> R.string.settings_field_saved
        FieldSaveStatus.FAILED -> R.string.entry_write_error
        null -> hintRes
    }
    Text(
        stringResource(message),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.labelMedium,
        color = if (validationError || status == FieldSaveStatus.FAILED) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
