package com.yusufcanmercan.weight_track_app.ui.settings

import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
}
