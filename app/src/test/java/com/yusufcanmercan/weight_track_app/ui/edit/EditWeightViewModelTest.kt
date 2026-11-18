package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.lifecycle.SavedStateHandle
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditWeightViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `gecerli kilo kaydedilir virgullu giris de kabul`() = runTest {
        val repo = FakeWeightRepository()
        val viewModel = EditWeightViewModel(repo, SavedStateHandle())

        viewModel.onWeightChange("80,5")
        viewModel.onSave()

        assertEquals(80.5, repo.observeAll().first().single().weight, 0.0)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `gecersiz kilo reddedilir ve kaydedilmez`() = runTest {
        val repo = FakeWeightRepository()
        val viewModel = EditWeightViewModel(repo, SavedStateHandle())

        for (text in listOf("", "abc", "0", "-5", "501")) {
            viewModel.onWeightChange(text)
            viewModel.onSave()
            assertTrue("'$text' reddedilmeli", viewModel.uiState.value.weightError)
        }

        assertTrue(repo.observeAll().first().isEmpty())
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `duzenleme yeni kayit eklemez mevcut kaydi gunceller`() = runTest {
        val repo = FakeWeightRepository(listOf(Weight(id = 7, weight = 80.0, timeStamp = 1_000L)))
        val viewModel = EditWeightViewModel(repo, SavedStateHandle(mapOf(EditWeightViewModel.ID_KEY to 7L)))
        assertEquals("80.0", viewModel.uiState.value.weightText)

        viewModel.onWeightChange("79.5")
        viewModel.onSave()

        val saved = repo.observeAll().first().single()
        assertEquals(7L, saved.id)
        assertEquals(79.5, saved.weight, 0.0)
    }

    @Test
    fun `not kirpilarak kaydedilir bos not null olur`() = runTest {
        val repo = FakeWeightRepository()
        val viewModel = EditWeightViewModel(repo, SavedStateHandle())
        viewModel.onWeightChange("80")
        viewModel.onNoteChange("  tatilden döndüm  ")
        viewModel.onSave()
        assertEquals("tatilden döndüm", repo.observeAll().first().single().note)

        val repo2 = FakeWeightRepository()
        val vm2 = EditWeightViewModel(repo2, SavedStateHandle())
        vm2.onWeightChange("80")
        vm2.onNoteChange("   ")
        vm2.onSave()
        assertEquals(null, repo2.observeAll().first().single().note)
    }

    @Test
    fun `not uzunlugu sinirlanir`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository(), SavedStateHandle())
        viewModel.onNoteChange("a".repeat(500))
        assertEquals(EditWeightViewModel.MAX_NOTE_LENGTH, viewModel.uiState.value.noteText.length)
    }

    @Test
    fun `duzenlemede mevcut not yuklenir ve guncellenir`() = runTest {
        val repo = FakeWeightRepository(listOf(Weight(id = 7, weight = 80.0, timeStamp = 1_000L, note = "eski")))
        val viewModel = EditWeightViewModel(repo, SavedStateHandle(mapOf(EditWeightViewModel.ID_KEY to 7L)))
        assertEquals("eski", viewModel.uiState.value.noteText)

        viewModel.onNoteChange("yeni")
        viewModel.onSave()
        assertEquals("yeni", repo.observeAll().first().single().note)
    }
}
