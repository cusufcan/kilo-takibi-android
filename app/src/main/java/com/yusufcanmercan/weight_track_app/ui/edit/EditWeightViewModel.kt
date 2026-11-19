package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.util.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
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
            dateMillis = startOfDay(System.currentTimeMillis()),
            isEditing = id != null,
        )
        loadJob = viewModelScope.launch {
            if (id != null) {
                repository.observeById(id).first()?.let { weight ->
                    original = weight
                    _uiState.update {
                        it.copy(
                            weightText = formatKg(weight.weight),
                            noteText = weight.note.orEmpty(),
                            dateMillis = startOfDay(weight.timeStamp),
                        )
                    }
                }
            } else {
                // Yeni kayıt son kiloyla başlar: çoğu gün yalnız birkaç kez −/+ yeter.
                repository.observeAll().first().maxByOrNull { it.timeStamp }?.let { last ->
                    _uiState.update { it.copy(weightText = formatKg(last.weight)) }
                }
            }
        }
    }

    fun onWeightChange(text: String) {
        _uiState.update { it.copy(weightText = text, weightError = false) }
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
        _uiState.update { it.copy(dateMillis = dateMillis) }
    }

    fun onSave() {
        val state = _uiState.value
        val value = parseKg(state.weightText)
        if (value == null || value !in MIN_KG..MAX_KG) {
            _uiState.update { it.copy(weightError = true) }
            return
        }
        // Boş/boşluk-only not kaydedilmez (null).
        val note = state.noteText.trim().ifEmpty { null }
        viewModelScope.launch {
            val old = original
            if (old != null) {
                repository.update(old.copy(weight = value, timeStamp = state.dateMillis, note = note))
            } else {
                repository.add(Weight(weight = value, timeStamp = state.dateMillis, note = note))
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Düzenlenen kaydı siler ve sayfayı kapatır. */
    fun onDelete() {
        val old = original ?: return
        viewModelScope.launch {
            repository.delete(old)
            _uiState.update { it.copy(saved = true) }
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
