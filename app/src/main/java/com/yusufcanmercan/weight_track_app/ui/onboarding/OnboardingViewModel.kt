package com.yusufcanmercan.weight_track_app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal enum class OnboardingPhase { CHECKING, CHECK_FAILED, QUESTIONS, SAVING, SAVE_FAILED, HOME }

internal data class OnboardingAnswers(
    val name: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goalKg: Double? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val weights: WeightRepository,
) : ViewModel() {
    private val _phase = MutableStateFlow(OnboardingPhase.CHECKING)
    internal val phase = _phase.asStateFlow()
    private var retryOperation: (() -> Unit)? = null

    init {
        checkOnboarding()
    }

    internal fun finish(answers: OnboardingAnswers) {
        if (_phase.value != OnboardingPhase.QUESTIONS) return
        val firstWeight = answers.weightKg?.let { kg ->
            Weight(weight = kg, timeStamp = System.currentTimeMillis(), hasMeasurementTime = true)
        }
        saveAnswers(answers, firstWeight)
    }

    internal fun retry() {
        if (_phase.value == OnboardingPhase.CHECK_FAILED || _phase.value == OnboardingPhase.SAVE_FAILED) {
            retryOperation?.invoke()
        }
    }

    private fun checkOnboarding() {
        retryOperation = ::checkOnboarding
        runOperation(OnboardingPhase.CHECKING, OnboardingPhase.CHECK_FAILED) {
            val state = settings.onboarding.first()
            when {
                state.pendingWeight != null -> saveFirstWeight(state.pendingWeight)
                state.completed -> return@runOperation OnboardingPhase.HOME
                weights.hasMeasurements() -> settings.completeOnboarding()
                else -> return@runOperation OnboardingPhase.QUESTIONS
            }
            OnboardingPhase.HOME
        }
    }

    private fun saveAnswers(answers: OnboardingAnswers, firstWeight: Weight?) {
        retryOperation = { saveAnswers(answers, firstWeight) }
        runOperation(OnboardingPhase.SAVING, OnboardingPhase.SAVE_FAILED) {
            settings.saveOnboarding(answers.name, answers.heightCm, answers.goalKg, firstWeight)
                ?.let { saveFirstWeight(it) }
            OnboardingPhase.HOME
        }
    }

    private suspend fun saveFirstWeight(weight: Weight) {
        weights.addFirst(weight)
        settings.completeOnboarding()
    }

    private fun runOperation(
        loading: OnboardingPhase,
        failed: OnboardingPhase,
        operation: suspend () -> OnboardingPhase,
    ) {
        _phase.value = loading
        viewModelScope.launch {
            _phase.value = try {
                operation().also { retryOperation = null }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed
            }
        }
    }
}
