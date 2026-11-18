package com.yusufcanmercan.weight_track_app.data.repository

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightRepositoryImplTest {

    private val repository = WeightRepositoryImpl(FakeWeightDao())

    @Test
    fun `eklenen kayit listede gorunur`() = runTest {
        repository.add(Weight(weight = 80.5, timeStamp = 1_000L))

        val list = repository.observeAll().first()

        assertEquals(1, list.size)
        assertEquals(80.5, list.single().weight, 0.0)
    }

    @Test
    fun `silinen kayit listeden kalkar`() = runTest {
        repository.add(Weight(weight = 80.0, timeStamp = 1_000L))
        val saved = repository.observeAll().first().single()

        repository.delete(saved)

        assertEquals(emptyList<Weight>(), repository.observeAll().first())
        assertNull(repository.observeById(saved.id).first())
    }

    @Test
    fun `guncellenen kilo yeni degerle gelir`() = runTest {
        repository.add(Weight(weight = 80.0, timeStamp = 1_000L))
        val saved = repository.observeAll().first().single()

        repository.update(saved.copy(weight = 79.2))

        assertEquals(79.2, repository.observeById(saved.id).first()?.weight ?: 0.0, 0.0)
    }
}
