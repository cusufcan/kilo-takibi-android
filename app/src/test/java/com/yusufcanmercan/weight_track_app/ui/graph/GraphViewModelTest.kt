package com.yusufcanmercan.weight_track_app.ui.graph

import com.yusufcanmercan.weight_track_app.data.settings.FakeSettingsRepository
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `baslangicta yukleniyor durumundadir`() {
        assertTrue(GraphViewModel(FakeWeightRepository(), FakeSettingsRepository()).uiState.value.isLoading)
    }

    @Test
    fun `kayit yoksa veri null olur`() = runTest {
        val viewModel = GraphViewModel(FakeWeightRepository(), FakeSettingsRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.data)
    }

    @Test
    fun `kayitlar grafik verisine donusur`() = runTest {
        val repo = FakeWeightRepository(
            listOf(
                Weight(id = 1, weight = 80.0, timeStamp = 0L),
                Weight(id = 2, weight = 79.0, timeStamp = 86_400_000L),
            ),
        )
        val viewModel = GraphViewModel(repo, FakeSettingsRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertEquals(2, viewModel.uiState.value.data!!.points.size)
    }
}
