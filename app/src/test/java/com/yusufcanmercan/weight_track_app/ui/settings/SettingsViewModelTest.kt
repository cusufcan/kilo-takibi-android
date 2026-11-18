package com.yusufcanmercan.weight_track_app.ui.settings

import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `baslangicta yukleniyor durumundadir`() {
        assertTrue(SettingsViewModel(FakeSettingsRepository()).uiState.value.isLoading)
    }

    @Test
    fun `kayitli tema duruma yansir`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository(ThemeMode.DARK))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `tema degisince durum guncellenir`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onThemeChange(ThemeMode.LIGHT)
        runCurrent()

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    private fun TestScope.vm(repo: FakeSettingsRepository = FakeSettingsRepository()): SettingsViewModel {
        val viewModel = SettingsViewModel(repo)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun `gecerli hedef kaydedilir virgullu da olur`() = runTest {
        val viewModel = vm()

        assertTrue(viewModel.onGoalSave("72,5"))
        runCurrent()

        assertEquals(72.5, viewModel.uiState.value.goalKg)
    }

    @Test
    fun `bos metin hedefi siler`() = runTest {
        val viewModel = vm(FakeSettingsRepository(initialGoal = 70.0))

        assertTrue(viewModel.onGoalSave("  "))
        runCurrent()

        assertEquals(null, viewModel.uiState.value.goalKg)
    }

    @Test
    fun `gecersiz hedef kaydedilmez`() = runTest {
        val viewModel = vm(FakeSettingsRepository(initialGoal = 70.0))

        assertFalse(viewModel.onGoalSave("abc"))
        assertFalse(viewModel.onGoalSave("0"))
        assertFalse(viewModel.onGoalSave("501"))
        runCurrent()

        assertEquals(70.0, viewModel.uiState.value.goalKg)
    }

    @Test
    fun `gecerli boy kaydedilir bos metin siler`() = runTest {
        val viewModel = vm()

        assertTrue(viewModel.onHeightSave("172,5"))
        runCurrent()
        assertEquals(172.5, viewModel.uiState.value.heightCm)

        assertTrue(viewModel.onHeightSave(" "))
        runCurrent()
        assertEquals(null, viewModel.uiState.value.heightCm)
    }

    @Test
    fun `gecersiz boy kaydedilmez`() = runTest {
        val viewModel = vm(FakeSettingsRepository(initialHeight = 170.0))

        assertFalse(viewModel.onHeightSave("abc"))
        assertFalse(viewModel.onHeightSave("99"))
        assertFalse(viewModel.onHeightSave("251"))
        runCurrent()

        assertEquals(170.0, viewModel.uiState.value.heightCm)
    }
}
