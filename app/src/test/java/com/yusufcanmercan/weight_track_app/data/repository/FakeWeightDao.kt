package com.yusufcanmercan.weight_track_app.data.repository

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.local.WeightDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Test için DAO'nun bellekte çalışan taklidi. Room'un yaptığı gibi id üretir ve değişince Flow'a yayar. */
class FakeWeightDao : WeightDao {
    private val rows = MutableStateFlow<List<Weight>>(emptyList())
    private var nextId = 1L

    // Gerçek sorgudaki ORDER BY timeStamp'in karşılığı.
    override fun observeAll(): Flow<List<Weight>> = rows.map { list -> list.sortedBy { it.timeStamp } }

    override fun observeById(id: Long): Flow<Weight?> = rows.map { list -> list.find { it.id == id } }

    override suspend fun hasMeasurements(): Boolean = rows.value.isNotEmpty()

    override suspend fun insert(weight: Weight): Long {
        val saved = weight.copy(id = nextId++)
        rows.value = rows.value + saved
        return saved.id
    }

    override suspend fun insertAll(weights: List<Weight>): List<Long> = weights.map { insert(it) }

    override suspend fun deleteIds(ids: List<Long>) {
        rows.value = rows.value.filterNot { it.id in ids }
    }

    override suspend fun update(weight: Weight) {
        rows.value = rows.value.map { if (it.id == weight.id) weight else it }
    }

    override suspend fun delete(weight: Weight) {
        rows.value = rows.value.filterNot { it.id == weight.id }
    }
}
