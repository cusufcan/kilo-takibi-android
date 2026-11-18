package com.yusufcanmercan.weight_track_app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Dilin tek kaynağı Android'in kendisi; ekran yalnız açılışta okur, seçince Activity zaten yenilenir.
    var language by remember { mutableStateOf(AppLanguage.current()) }

    SettingsContent(
        state = state,
        language = language,
        onThemeChange = viewModel::onThemeChange,
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
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Section(R.string.settings_theme) {
            ThemeMode.entries.forEach { mode ->
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
            AppLanguage.entries.forEach { lang ->
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

@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
    content()
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
        Text(label)
    }
}
