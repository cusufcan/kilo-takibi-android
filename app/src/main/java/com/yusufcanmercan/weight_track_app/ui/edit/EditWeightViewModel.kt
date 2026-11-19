package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.util.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `id` rotadan SavedStateHandle ile gelir (ADR-0004: yalnız id taşınır). id yoksa yeni kayıt,
 * varsa o kaydı düzenler; kayıt Room'dan okunur (tek doğruluk kaynağı).
 */
@HiltViewModel
class EditWeightViewModel @Inject constructor(
    private val repository: WeightRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editedId: Long? = savedStateHandle[ID_KEY]

    private val _uiState = MutableStateFlow(
        EditUiState(dateMillis = startOfDay(System.currentTimeMillis()), isEditing = editedId != null),
    )
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private var original: Weight? = null

    init {
        if (editedId != null) {
            viewModelScope.launch {
                repository.observeById(editedId).first()?.let { weight ->
                    original = weight
                    _uiState.update {
                        it.copy(
                            weightText = weight.weight.toString(),
                            noteText = weight.note.orEmpty(),
                            dateMillis = startOfDay(weight.timeStamp),
                        )
                    }
                }
            }
        }
    }

    fun onWeightChange(text: String) {
        _uiState.update { it.copy(weightText = text, weightError = false) }
    }

    fun onNoteChange(text: String) {
        _uiState.update { it.copy(noteText = text.take(MAX_NOTE_LENGTH)) }
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.update { it.copy(dateMillis = dateMillis) }
    }

    fun onSave() {
        val state = _uiState.value
        // Virgüllü giriş ("80,5") da kabul.
        val value = state.weightText.replace(',', '.').toDoubleOrNull()
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

    /** Düzenlenen kaydı siler ve ekranı kapatır (silme ana sayfadan buraya taşındı, tasarim.md v2). */
    fun onDelete() {
        val old = original ?: return
        viewModelScope.launch {
            repository.delete(old)
            _uiState.update { it.copy(saved = true) }
        }
    }

    companion object {
        const val ID_KEY = "id"
        const val MIN_KG = 1.0
        const val MAX_KG = 500.0
        const val MAX_NOTE_LENGTH = 200
    }
}
