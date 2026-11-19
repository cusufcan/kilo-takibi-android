package com.yusufcanmercan.weight_track_app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Dilin tek kaynağı Android'in kendisi; ekran yalnız açılışta okur, seçince Activity zaten yenilenir.
    var language by remember { mutableStateOf(AppLanguage.current()) }

    SettingsContent(
        state = state,
        language = language,
        onThemeChange = viewModel::onThemeChange,
        onGoalSave = viewModel::onGoalSave,
        onHeightSave = viewModel::onHeightSave,
        onLanguageChange = {
            language = it
            AppLanguage.apply(it)
        },
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    language: AppLanguage,
    onThemeChange: (ThemeMode) -> Unit,
    onGoalSave: (String) -> Boolean,
    onHeightSave: (String) -> Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)
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
        Section(R.string.settings_goal) {
            NumberField(
                value = state.goalKg,
                labelRes = R.string.settings_goal_field,
                hintRes = R.string.settings_goal_hint,
                invalidRes = R.string.settings_goal_invalid,
                onSave = onGoalSave,
            )
        }
        Section(R.string.settings_height) {
            NumberField(
                value = state.heightCm,
                labelRes = R.string.settings_height_field,
                hintRes = R.string.settings_height_hint,
                invalidRes = R.string.settings_height_invalid,
                onSave = onHeightSave,
            )
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
private fun NumberField(value: Double?, labelRes: Int, hintRes: Int, invalidRes: Int, onSave: (String) -> Boolean) {
    // Kayıtlı değer değişince (ör. ilk yükleme) metin ona eşitlenir.
    var text by remember(value) { mutableStateOf(value?.let { "%.1f".format(Locale.US, it) }.orEmpty()) }
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
                    onValueChange = { text = it; error = false },
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
            FilledTonalButton(onClick = { error = !onSave(text) }) {
                Text(stringResource(R.string.settings_goal_save))
            }
        }
        Text(
            text = stringResource(if (error) invalidRes else hintRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
