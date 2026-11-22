package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.local.hasKnownMeasurementTime
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Ekle/düzenle alt sayfasının ViewModel'i. Alt sayfa bir rota olmadığı için `id` rotadan gelmez;
 * sayfa her açıldığında `start(id)` çağrılır (ADR-0015). `id` null ise yeni kayıt (son kiloyla dolu),
 * değilse o kayıt Room'dan okunur (tek doğruluk kaynağı).
 */
@HiltViewModel
class EditWeightViewModel @Inject constructor(
    private val repository: WeightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private var original: Weight? = null
    private var loadJob: Job? = null

    /**
     * Bu ViewModel bir kez kuruldu mu? Döndürmede ViewModel yaşar (true kalır, yazılan silinmez);
     * süreç öldürülüp geri gelince yenisi false başlar, ekran `start`'ı yeniden çağırır.
     */
    var isStarted = false
        private set

    /** Sayfayı sıfırdan kurar; önceki açılıştan kalan durum ve yarım yükleme silinir. */
    fun start(id: Long?) {
        isStarted = true
        loadJob?.cancel()
        original = null
        _uiState.value = EditUiState(
            dateMillis = System.currentTimeMillis(),
            hasMeasurementTime = id == null,
            isEditing = id != null,
            busy = true,
        )
        loadJob = viewModelScope.launch {
            try {
                if (id != null) {
                    val weight = repository.observeById(id).first()
                    original = weight
                    _uiState.update {
                        if (weight == null) it.copy(available = false) else it.copy(
                            weightText = formatKg(weight.weight),
                            noteText = weight.note.orEmpty(),
                            dateMillis = weight.timeStamp,
                            hasMeasurementTime = weight.hasKnownMeasurementTime(),
                            mood = EntryMood.entries.firstOrNull { mood -> mood.name == weight.mood },
                        )
                    }
                } else {
                    repository.observeAll().first().maxWithOrNull(compareBy(Weight::timeStamp, Weight::id))?.let { last ->
                        _uiState.update { it.copy(weightText = formatKg(last.weight)) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(available = id == null, writeError = true) }
            }
            _uiState.update { it.copy(busy = false) }
        }
    }

    fun onWeightChange(text: String) {
        _uiState.update { it.copy(weightText = text, weightError = false) }
    }

    fun onRulerChange(value: Double) {
        val rounded = (value.coerceIn(MIN_KG, MAX_KG) * 10).roundToLong() / 10.0
        onWeightChange(formatKg(rounded))
    }

    /** −/+ düğmeleri: `delta` kadar oynatır, 0,1'e yuvarlar, 1–500 dışına taşmaz. Boş alanda işlemez. */
    fun onStep(delta: Double) {
        val current = parseKg(_uiState.value.weightText) ?: return
        val next = ((current + delta) * 10).roundToLong() / 10.0
        onWeightChange(formatKg(next.coerceIn(MIN_KG, MAX_KG)))
    }

    fun onNoteChange(text: String) {
        _uiState.update { it.copy(noteText = text.take(MAX_NOTE_LENGTH)) }
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.update { state ->
            val day = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val measuredAt = Calendar.getInstance().apply {
                timeInMillis = state.dateMillis
                set(day[Calendar.YEAR], day[Calendar.MONTH], day[Calendar.DAY_OF_MONTH])
            }.timeInMillis
            state.copy(dateMillis = measuredAt)
        }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        _uiState.update { state ->
            val measuredAt = Calendar.getInstance().apply {
                timeInMillis = state.dateMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            state.copy(dateMillis = measuredAt, hasMeasurementTime = true)
        }
    }

    fun onMoodChange(mood: EntryMood) {
        _uiState.update { it.copy(mood = mood.takeUnless { selected -> selected == it.mood }) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.busy || state.saved || (state.isEditing && original == null)) return
        val value = parseKg(state.weightText)
        if (value == null || value !in MIN_KG..MAX_KG) {
            _uiState.update { it.copy(weightError = true) }
            return
        }
        val note = state.noteText.trim().ifEmpty { null }
        val old = original
        write {
            val weight = Weight(
                id = old?.id ?: 0L,
                weight = value,
                timeStamp = state.dateMillis,
                note = note,
                mood = state.mood?.name,
                hasMeasurementTime = state.hasMeasurementTime,
            )
            if (old != null) {
                repository.update(weight)
                null
            } else {
                repository.add(weight)
            }
        }
    }

    /** Düzenlenen kaydı siler ve sayfayı kapatır. */
    fun onDelete() {
        val old = original ?: return
        if (_uiState.value.busy || _uiState.value.saved) return
        write {
            repository.delete(old)
            null
        }
    }

    private fun write(operation: suspend () -> Long?) {
        _uiState.update { it.copy(busy = true, writeError = false) }
        viewModelScope.launch {
            try {
                val id = operation()
                _uiState.update { it.copy(saved = true, savedEntryId = id, busy = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(busy = false, writeError = true) }
            }
        }
    }

    /** Virgüllü giriş ("80,5") de kabul. */
    private fun parseKg(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    /**
     * Sayıyı alana yazılacak metne çevirir; ayraç cihaz dilinden gelir (TR "67,1", EN "67.1"),
     * ana sayfadaki gösterimle aynı olsun diye. Yuvarlamaz: 80.25 "80,25" kalır.
     */
    private fun formatKg(value: Double): String =
        value.toString().replace('.', DecimalFormatSymbols.getInstance().decimalSeparator)

    companion object {
        const val MIN_KG = 1.0
        const val MAX_KG = 500.0
        const val STEP_KG = 0.1
        const val MAX_NOTE_LENGTH = 200
    }
}
