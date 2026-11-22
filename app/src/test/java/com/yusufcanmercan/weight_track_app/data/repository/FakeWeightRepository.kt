package com.yusufcanmercan.weight_track_app.data.repository

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** ViewModel testleri için Repository'nin bellekteki sahtesi. */
class FakeWeightRepository(initial: List<Weight> = emptyList()) : WeightRepository {
    private val rows = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Weight>> = rows.map { list -> list.sortedBy { it.timeStamp } }

    override fun observeById(id: Long): Flow<Weight?> = rows.map { list -> list.find { it.id == id } }

    override suspend fun add(weight: Weight): Long {
        val id = (rows.value.maxOfOrNull { it.id } ?: 0L) + 1
        rows.value = rows.value + weight.copy(id = id)
        return id
    }

    override suspend fun hasMeasurements(): Boolean = rows.value.isNotEmpty()

    override suspend fun addFirst(weight: Weight) {
        if (rows.value.isEmpty()) add(weight)
    }

    override suspend fun update(weight: Weight) {
        rows.value = rows.value.map { if (it.id == weight.id) weight else it }
    }

    override suspend fun delete(weight: Weight) {
        rows.value = rows.value.filterNot { it.id == weight.id }
    }
}
