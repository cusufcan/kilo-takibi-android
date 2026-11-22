package com.yusufcanmercan.weight_track_app.ui.onboarding

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `mevcut kayitla tur acilmaz ve bu karar saklanir`() = runTest {
        val entry = Weight(id = 4, weight = 80.0, timeStamp = 123L)
        val weights = FakeWeightRepository(listOf(entry))
        val settings = FakeSettingsRepository()
        val viewModel = OnboardingViewModel(settings, weights)
        runCurrent()

        assertEquals(OnboardingPhase.HOME, viewModel.phase.value)
        assertTrue(settings.onboarding.first().completed)
        assertEquals(listOf(entry), weights.observeAll().first())
        weights.delete(entry)

        val reopened = OnboardingViewModel(settings, weights)
        runCurrent()
        assertEquals(OnboardingPhase.HOME, reopened.phase.value)
    }

    @Test
    fun `turu gecmek tamamlanmayi bos gecmiste de korur`() = runTest {
        val settings = FakeSettingsRepository(initialHeight = 170.0)
        val weights = FakeWeightRepository()
        val viewModel = OnboardingViewModel(settings, weights)
        runCurrent()
        assertEquals(OnboardingPhase.QUESTIONS, viewModel.phase.value)

        viewModel.finish(OnboardingAnswers())
        runCurrent()

        val reopened = OnboardingViewModel(settings, weights)
        runCurrent()
        assertEquals(OnboardingPhase.HOME, reopened.phase.value)
        assertTrue(weights.observeAll().first().isEmpty())
        assertEquals(170.0, settings.heightCm.first())
    }

    @Test
    fun `ikinci acik tur ilk gonderimin bilgilerini degistirmez`() = runTest {
        val settings = FakeSettingsRepository()
        val weights = FakeWeightRepository()
        val first = OnboardingViewModel(settings, weights)
        val second = OnboardingViewModel(settings, weights)
        runCurrent()

        first.finish(OnboardingAnswers(name = "Yusuf", weightKg = 81.2))
        second.finish(OnboardingAnswers(name = "Deneme", weightKg = 77.0))
        runCurrent()

        assertEquals(OnboardingPhase.HOME, first.phase.value)
        assertEquals(OnboardingPhase.HOME, second.phase.value)
        assertEquals("Yusuf", settings.displayName.first())
        assertEquals(81.2, weights.observeAll().first().single().weight, 0.0)
    }

    @Test
    fun `son yazim kesilirse yeniden acilis ilk tartiyi cogaltmaz`() = runTest {
        val settings = FakeSettingsRepository()
        val interrupted = object : SettingsRepository by settings {
            override suspend fun completeOnboarding() = throw IOException("Interrupted")
        }
        val weights = FakeWeightRepository()
        val viewModel = OnboardingViewModel(interrupted, weights)
        runCurrent()

        viewModel.finish(OnboardingAnswers(name = "Yusuf", heightCm = 176.5, weightKg = 81.2, goalKg = 75.0))
        runCurrent()
        assertEquals(OnboardingPhase.SAVE_FAILED, viewModel.phase.value)
        val first = weights.observeAll().first().single()
        assertEquals(81.2, first.weight, 0.0)
        assertEquals(first.timeStamp, settings.onboarding.first().pendingWeight?.timeStamp)

        val reopened = OnboardingViewModel(settings, weights)
        runCurrent()

        assertEquals(OnboardingPhase.HOME, reopened.phase.value)
        assertEquals(listOf(first), weights.observeAll().first())
        assertEquals("Yusuf", settings.displayName.first())
        assertEquals(176.5, settings.heightCm.first())
        assertEquals(75.0, settings.goalWeight.first())
        assertTrue(settings.onboarding.first().completed)
        assertNull(settings.onboarding.first().pendingWeight)
    }
}
