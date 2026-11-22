package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `baslangicta yukleniyor durumundadir`() {
        val viewModel = HomeViewModel(FakeWeightRepository(), FakeSettingsRepository())

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `kayitlar en yeni uste gelecek sekilde listelenir`() = runTest {
        val repo = FakeWeightRepository(
            listOf(
                Weight(id = 1, weight = 80.0, timeStamp = 1_000L),
                Weight(id = 2, weight = 79.0, timeStamp = 3_000L),
                Weight(id = 3, weight = 79.5, timeStamp = 2_000L),
            ),
        )
        val viewModel = HomeViewModel(repo, FakeSettingsRepository())
        val state = viewModel.uiState.first { !it.isLoading }

        assertFalse(state.isLoading)
        assertEquals(listOf(2L, 3L, 1L), state.items.map { it.weight.id })
    }

    @Test
    fun `hedef ve boy degisince kayit gecmisi yeniden uretilmez`() = runTest {
        val settings = FakeSettingsRepository()
        val repo = FakeWeightRepository(listOf(Weight(1, 80.0, 1_000L), Weight(2, 75.0, 86_401_000L)))
        val viewModel = HomeViewModel(repo, settings)
        backgroundScope.launch { viewModel.uiState.collect {} }
        val before = viewModel.uiState.first { !it.isLoading }

        settings.setGoalWeight(70.0)
        settings.setHeightCm(175.0)
        val after = viewModel.uiState.first { it.goal?.goalKg == 70.0 && it.heightCm == 175.0 }

        assertSame(before.items, after.items)
        assertSame(before.dailyWeights, after.dailyWeights)
        assertSame(before.months, after.months)
        assertEquals(calculateBmi(75.0, 175.0), after.bmi)
    }
}
