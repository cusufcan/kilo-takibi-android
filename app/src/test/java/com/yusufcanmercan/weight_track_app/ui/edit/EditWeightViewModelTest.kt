package com.yusufcanmercan.weight_track_app.ui.edit

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.FakeWeightRepository
import com.yusufcanmercan.weight_track_app.ui.home.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import java.util.Calendar

class EditWeightViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // Ondalık ayraç cihaz dilinden gelir; testler makinenin diline bağlı kalmasın.
    private val savedLocale = Locale.getDefault()

    @Before
    fun setUp() = Locale.setDefault(Locale.US)

    @After
    fun tearDown() = Locale.setDefault(savedLocale)

    @Test
    fun `gecerli kilo kaydedilir virgullu giris de kabul`() = runTest {
        val repo = FakeWeightRepository()
        val viewModel = EditWeightViewModel(repo).apply { start(null) }

        viewModel.onWeightChange("80,5")
        viewModel.onSave()

        assertEquals(80.5, repo.observeAll().first().single().weight, 0.0)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `yeni kayit saati ve duyguyu saklar ve ayni kaydi iki kez eklemez`() = runTest {
        val repo = FakeWeightRepository()
        val viewModel = EditWeightViewModel(repo).apply { start(null) }

        viewModel.onRulerChange(76.14)
        viewModel.onTimeChange(0, 0)
        viewModel.onMoodChange(EntryMood.CALM)
        viewModel.onSave()
        viewModel.onSave()

        val saved = repo.observeAll().first().single()
        assertEquals(76.1, saved.weight, 0.0)
        assertEquals("CALM", saved.mood)
        assertTrue(saved.hasMeasurementTime)
        assertEquals(0, Calendar.getInstance().apply { timeInMillis = saved.timeStamp }[Calendar.HOUR_OF_DAY])
        assertEquals(saved.id, viewModel.uiState.value.savedEntryId)
    }

    @Test
    fun `gelecek gun secilemez gecmis gun secilebilir`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository()).apply { start(null) }
        val before = viewModel.uiState.value.dateMillis
        val day = 24L * 60 * 60 * 1000

        viewModel.onDateChange(before + 2 * day)
        assertEquals(before, viewModel.uiState.value.dateMillis)

        viewModel.onDateChange(before - 3 * day)
        assertTrue(viewModel.uiState.value.dateMillis < before - 2 * day)
    }

    @Test
    fun `gecersiz kilo reddedilir ve kaydedilmez`() = runTest {
        val repo = FakeWeightRepository()
        val viewModel = EditWeightViewModel(repo).apply { start(null) }

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
        val viewModel = EditWeightViewModel(repo).apply { start(7L) }
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
        val viewModel = EditWeightViewModel(repo).apply { start(null) }
        viewModel.onWeightChange("80")
        viewModel.onNoteChange("  tatilden döndüm  ")
        viewModel.onSave()
        assertEquals("tatilden döndüm", repo.observeAll().first().single().note)

        val repo2 = FakeWeightRepository()
        val vm2 = EditWeightViewModel(repo2).apply { start(null) }
        vm2.onWeightChange("80")
        vm2.onNoteChange("   ")
        vm2.onSave()
        assertEquals(null, repo2.observeAll().first().single().note)
    }

    @Test
    fun `not uzunlugu sinirlanir`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository()).apply { start(null) }
        viewModel.onNoteChange("a".repeat(500))
        assertEquals(EditWeightViewModel.MAX_NOTE_LENGTH, viewModel.uiState.value.noteText.length)
    }

    @Test
    fun `duzenlemede mevcut not yuklenir ve guncellenir`() = runTest {
        val repo = FakeWeightRepository(listOf(Weight(id = 7, weight = 80.0, timeStamp = 1_000L, note = "eski")))
        val viewModel = EditWeightViewModel(repo).apply { start(7L) }
        assertEquals("eski", viewModel.uiState.value.noteText)

        viewModel.onNoteChange("yeni")
        viewModel.onSave()
        assertEquals("yeni", repo.observeAll().first().single().note)
    }

    @Test
    fun `duzenlenen kayit silinir ve ekran kapanir`() = runTest {
        val repo = FakeWeightRepository(listOf(Weight(id = 7, weight = 80.0, timeStamp = 1_000L)))
        val viewModel = EditWeightViewModel(repo).apply { start(7L) }

        viewModel.onDelete()

        assertTrue(repo.observeAll().first().isEmpty())
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `yeni kayitta silme hicbir sey yapmaz`() = runTest {
        val repo = FakeWeightRepository(listOf(Weight(id = 7, weight = 80.0, timeStamp = 1_000L)))
        val viewModel = EditWeightViewModel(repo).apply { start(null) }

        viewModel.onDelete()

        assertEquals(1, repo.observeAll().first().size)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `yeni kayit en son kiloyla dolu baslar`() = runTest {
        val repo = FakeWeightRepository(
            listOf(
                Weight(id = 1, weight = 82.0, timeStamp = 1_000L),
                Weight(id = 2, weight = 80.4, timeStamp = 3_000L),
                Weight(id = 3, weight = 81.0, timeStamp = 2_000L),
            ),
        )
        val viewModel = EditWeightViewModel(repo).apply { start(null) }

        assertEquals("80.4", viewModel.uiState.value.weightText)
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `hic kayit yoksa kilo alani bos baslar`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository()).apply { start(null) }

        assertEquals("", viewModel.uiState.value.weightText)
    }

    @Test
    fun `arti eksi onda bir oynatir ve yuvarlar`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository()).apply { start(null) }
        viewModel.onWeightChange("80")

        viewModel.onStep(EditWeightViewModel.STEP_KG)
        assertEquals("80.1", viewModel.uiState.value.weightText)
        repeat(3) { viewModel.onStep(-EditWeightViewModel.STEP_KG) }
        assertEquals("79.8", viewModel.uiState.value.weightText)
    }

    @Test
    fun `arti eksi sinirlarin disina tasmaz`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository()).apply { start(null) }

        viewModel.onWeightChange("500")
        viewModel.onStep(EditWeightViewModel.STEP_KG)
        assertEquals("500.0", viewModel.uiState.value.weightText)

        viewModel.onWeightChange("1")
        viewModel.onStep(-EditWeightViewModel.STEP_KG)
        assertEquals("1.0", viewModel.uiState.value.weightText)
    }

    @Test
    fun `bos ya da gecersiz alanda arti eksi bir sey yapmaz`() = runTest {
        val viewModel = EditWeightViewModel(FakeWeightRepository()).apply { start(null) }

        viewModel.onStep(EditWeightViewModel.STEP_KG)
        assertEquals("", viewModel.uiState.value.weightText)

        viewModel.onWeightChange("abc")
        viewModel.onStep(EditWeightViewModel.STEP_KG)
        assertEquals("abc", viewModel.uiState.value.weightText)
    }

    @Test
    fun `start onceki acilistan kalan durumu siler`() = runTest {
        val repo = FakeWeightRepository(listOf(Weight(id = 7, weight = 80.0, timeStamp = 1_000L, note = "eski")))
        val viewModel = EditWeightViewModel(repo).apply { start(7L) }
        viewModel.onSave()
        assertTrue(viewModel.uiState.value.saved)

        viewModel.start(null)

        val state = viewModel.uiState.value
        assertFalse(state.saved)
        assertFalse(state.isEditing)
        assertEquals("", state.noteText)
        assertTrue(viewModel.isStarted)
    }

    @Test
    fun `turkcede kilo virgulle gosterilir`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        val repo = FakeWeightRepository(listOf(Weight(id = 1, weight = 67.0, timeStamp = 1_000L)))
        val viewModel = EditWeightViewModel(repo).apply { start(null) }
        assertEquals("67,0", viewModel.uiState.value.weightText)

        viewModel.onStep(EditWeightViewModel.STEP_KG)
        assertEquals("67,1", viewModel.uiState.value.weightText)

        viewModel.onSave()
        assertEquals(67.1, repo.observeAll().first().maxBy { it.id }.weight, 0.0)
    }
}
