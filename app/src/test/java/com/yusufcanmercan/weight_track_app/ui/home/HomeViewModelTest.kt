package com.yusufcanmercan.weight_track_app.ui.home

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
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
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf(2L, 3L, 1L), state.items.map { it.weight.id })
    }
}
