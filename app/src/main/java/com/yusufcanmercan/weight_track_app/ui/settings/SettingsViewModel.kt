package com.yusufcanmercan.weight_track_app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.export.CsvImportPlan
import com.yusufcanmercan.weight_track_app.data.export.MAX_IMPORT_BYTES
import com.yusufcanmercan.weight_track_app.data.export.planWeightCsvImport
import com.yusufcanmercan.weight_track_app.data.export.writeWeightCsv
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.data.settings.DailyReminder
import com.yusufcanmercan.weight_track_app.platform.ReminderScheduler
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val goalKg: Double? = null,
    val heightCm: Double? = null,
    val displayName: String? = null,
    val weeklySummaryEnabled: Boolean = false,
    val reminder: DailyReminder = DailyReminder(),
)

internal enum class SettingsField { GOAL, HEIGHT, NAME }
internal enum class FieldSaveStatus { WRITING, SAVED, FAILED }

internal enum class CsvExportStatus { IDLE, WRITING, SAVED, FAILED }

internal enum class CsvImportError { TOO_LARGE, UNREADABLE, WRITE }

/** İçe aktarma akışı: dosya okunur → önizleme → onay → yazılır → (isteğe bağlı) geri alınır. */
internal sealed interface CsvImportState {
    data object Idle : CsvImportState
    data object Reading : CsvImportState
    data class Preview(val plan: CsvImportPlan) : CsvImportState
    data object Writing : CsvImportState
    data class Done(val ids: List<Long>) : CsvImportState
    data object Undone : CsvImportState
    data class Failed(val error: CsvImportError) : CsvImportState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val weightRepository: WeightRepository,
    private val reminders: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            repository.themeMode, repository.goalWeight, repository.heightCm, repository.displayName,
            repository.weeklySummaryEnabled,
        ) { theme, goal, height, name, weekly ->
            SettingsUiState(
                isLoading = false, themeMode = theme, goalKg = goal, heightCm = height, displayName = name,
                weeklySummaryEnabled = weekly,
            )
        },
        repository.dailyReminder,
    ) { state, reminder -> state.copy(reminder = reminder) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _csvExportStatus = MutableStateFlow(CsvExportStatus.IDLE)
    internal val csvExportStatus = _csvExportStatus.asStateFlow()

    internal fun clearCsvExportStatus() {
        if (_csvExportStatus.value != CsvExportStatus.WRITING) _csvExportStatus.value = CsvExportStatus.IDLE
    }

    internal fun exportCsv(openDestination: () -> OutputStream?) {
        if (_csvExportStatus.value == CsvExportStatus.WRITING) return
        _csvExportStatus.value = CsvExportStatus.WRITING
        viewModelScope.launch {
            _csvExportStatus.value = try {
                val measurements = weightRepository.observeAll().first()
                withContext(Dispatchers.IO) {
                    val destination = openDestination() ?: throw IOException("CSV destination unavailable")
                    destination.bufferedWriter(Charsets.UTF_8).use { writeWeightCsv(it, measurements) }
                }
                CsvExportStatus.SAVED
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                CsvExportStatus.FAILED
            }
        }
    }

    private val _csvImport = MutableStateFlow<CsvImportState>(CsvImportState.Idle)
    internal val csvImport = _csvImport.asStateFlow()

    private val importBusy get() = _csvImport.value.let {
        it == CsvImportState.Reading || it == CsvImportState.Writing
    }

    internal fun readCsv(openSource: () -> InputStream?, now: Long = System.currentTimeMillis()) {
        if (importBusy) return
        _csvImport.value = CsvImportState.Reading
        viewModelScope.launch {
            _csvImport.value = try {
                val text = withContext(Dispatchers.IO) {
                    val source = openSource() ?: throw IOException("CSV source unavailable")
                    source.use { readLimited(it) }
                }
                if (text == null) {
                    CsvImportState.Failed(CsvImportError.TOO_LARGE)
                } else {
                    val existing = weightRepository.observeAll().first()
                    CsvImportState.Preview(withContext(Dispatchers.Default) { planWeightCsvImport(text, existing, now) })
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                CsvImportState.Failed(CsvImportError.UNREADABLE)
            }
        }
    }

    internal fun confirmImport() {
        val preview = _csvImport.value as? CsvImportState.Preview ?: return
        if (preview.plan.added.isEmpty()) return cancelImport()
        _csvImport.value = CsvImportState.Writing
        viewModelScope.launch {
            _csvImport.value = try {
                CsvImportState.Done(weightRepository.addAll(preview.plan.added))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                CsvImportState.Failed(CsvImportError.WRITE)
            }
        }
    }

    internal fun undoImport() {
        val done = _csvImport.value as? CsvImportState.Done ?: return
        _csvImport.value = CsvImportState.Writing
        viewModelScope.launch {
            _csvImport.value = try {
                weightRepository.deleteAll(done.ids)
                CsvImportState.Undone
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                done
            }
        }
    }

    internal fun cancelImport() {
        if (_csvImport.value is CsvImportState.Preview) _csvImport.value = CsvImportState.Idle
    }

    internal fun clearImportStatus() {
        if (!importBusy && _csvImport.value !is CsvImportState.Preview) _csvImport.value = CsvImportState.Idle
    }

    fun onWeeklySummaryChange(enabled: Boolean) {
        viewModelScope.launch {
            repository.setWeeklySummaryEnabled(enabled)
            reminders.sync()
        }
    }

    fun onReminderEnabledChange(enabled: Boolean) = updateReminder { it.copy(enabled = enabled) }

    fun onReminderTimeChange(hour: Int, minute: Int) = updateReminder { it.copy(minuteOfDay = hour * 60 + minute) }

    private fun updateReminder(change: (DailyReminder) -> DailyReminder) {
        viewModelScope.launch {
            repository.setDailyReminder(change(repository.dailyReminder.first()))
            reminders.sync()
        }
    }

    fun onThemeChange(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    private val _fieldStatus = MutableStateFlow<Map<SettingsField, FieldSaveStatus>>(emptyMap())
    internal val fieldStatus = _fieldStatus.asStateFlow()

    internal fun clearFieldStatus(field: SettingsField) {
        if (_fieldStatus.value[field] != FieldSaveStatus.WRITING) {
            _fieldStatus.value = _fieldStatus.value - field
        }
    }

    private fun saveField(field: SettingsField, write: suspend () -> Unit) {
        if (_fieldStatus.value[field] == FieldSaveStatus.WRITING) return
        _fieldStatus.value = _fieldStatus.value + (field to FieldSaveStatus.WRITING)
        viewModelScope.launch {
            val status = try {
                write()
                FieldSaveStatus.SAVED
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                FieldSaveStatus.FAILED
            }
            _fieldStatus.value = _fieldStatus.value + (field to status)
        }
    }

    fun onNameSave(value: String) {
        val name = value.trim().take(MAX_NAME_LENGTH).takeIf { it.isNotBlank() }
        saveField(SettingsField.NAME) { repository.setDisplayName(name) }
    }

    /** Boş metin hedefi siler. Geçersiz değer yazılmaz. */
    fun onGoalSave(text: String): Boolean {
        val value = text.replace(',', '.').toDoubleOrNull()
        if (text.isNotBlank() && (value == null || value !in EditWeightViewModel.MIN_KG..EditWeightViewModel.MAX_KG)) {
            return false
        }
        saveField(SettingsField.GOAL) { repository.setGoalWeight(value) }
        return true
    }

    /** Boş metin boyu siler. Geçersiz değer yazılmaz. */
    fun onHeightSave(text: String): Boolean {
        val value = text.replace(',', '.').toDoubleOrNull()
        if (text.isNotBlank() && (value == null || value !in MIN_HEIGHT_CM..MAX_HEIGHT_CM)) return false
        saveField(SettingsField.HEIGHT) { repository.setHeightCm(value) }
        return true
    }

    /** Sınırı aşan dosyada `null`; metin UTF-8 okunur. */
    private fun readLimited(source: InputStream): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = source.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_IMPORT_BYTES) return null
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val MIN_HEIGHT_CM = 100.0
        const val MAX_HEIGHT_CM = 250.0
        const val MAX_NAME_LENGTH = 40
    }
}
