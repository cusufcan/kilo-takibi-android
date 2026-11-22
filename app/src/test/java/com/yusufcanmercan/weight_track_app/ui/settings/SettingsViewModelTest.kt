package com.yusufcanmercan.weight_track_app.ui.settings

import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.DailyReminder
import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode
import com.yusufcanmercan.weight_track_app.platform.FakeReminderScheduler
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
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
        assertTrue(SettingsViewModel(FakeSettingsRepository(), FakeWeightRepository(), FakeReminderScheduler()).uiState.value.isLoading)
    }

    @Test
    fun `kayitli tema duruma yansir`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository(ThemeMode.DARK), FakeWeightRepository(), FakeReminderScheduler())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `tema degisince durum guncellenir`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), FakeWeightRepository(), FakeReminderScheduler())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onThemeChange(ThemeMode.LIGHT)
        runCurrent()

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    private fun TestScope.vm(repo: FakeSettingsRepository = FakeSettingsRepository()): SettingsViewModel {
        val viewModel = SettingsViewModel(repo, FakeWeightRepository(), FakeReminderScheduler())
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

    @Test
    fun `ayar yazilirken tekrar kaydedilmez ve basari yazma sonunda gorunur`() = runTest {
        val stored = FakeSettingsRepository()
        val mayFinish = CompletableDeferred<Unit>()
        val repo = object : SettingsRepository by stored {
            override suspend fun setGoalWeight(kg: Double?) {
                mayFinish.await()
                stored.setGoalWeight(kg)
            }
        }
        val viewModel = SettingsViewModel(repo, FakeWeightRepository(), FakeReminderScheduler())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onGoalSave("74")
        viewModel.onGoalSave("76")
        assertEquals(FieldSaveStatus.WRITING, viewModel.fieldStatus.value[SettingsField.GOAL])
        mayFinish.complete(Unit)
        runCurrent()

        assertEquals(74.0, viewModel.uiState.value.goalKg)
        assertEquals(FieldSaveStatus.SAVED, viewModel.fieldStatus.value[SettingsField.GOAL])
    }

    @Test
    fun `haftalik ozet acilinca kaydedilir ve alarm yeniden kurulur`() = runTest {
        val scheduler = FakeReminderScheduler()
        val viewModel = SettingsViewModel(FakeSettingsRepository(), FakeWeightRepository(), scheduler)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertFalse(viewModel.uiState.value.weeklySummaryEnabled)

        viewModel.onWeeklySummaryChange(true)
        runCurrent()

        assertTrue(viewModel.uiState.value.weeklySummaryEnabled)
        assertEquals(1, scheduler.syncCount)
    }

    @Test
    fun `gunluk hatirlatici varsayilan kapali 0800 acilinca ve saat degisince kaydedilip kurulur`() = runTest {
        val scheduler = FakeReminderScheduler()
        val viewModel = SettingsViewModel(FakeSettingsRepository(), FakeWeightRepository(), scheduler)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(DailyReminder(enabled = false, minuteOfDay = 8 * 60), viewModel.uiState.value.reminder)

        viewModel.onReminderEnabledChange(true)
        runCurrent()
        viewModel.onReminderTimeChange(21, 30)
        runCurrent()

        assertEquals(DailyReminder(enabled = true, minuteOfDay = 21 * 60 + 30), viewModel.uiState.value.reminder)
        assertEquals(2, scheduler.syncCount)
    }
}
