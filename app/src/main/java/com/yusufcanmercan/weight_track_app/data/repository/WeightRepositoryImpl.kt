package com.yusufcanmercan.weight_track_app.data.repository

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.local.WeightDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Repository'nin Room'lu gerçek hali. DAO'yu constructor'dan alır (ADR-0003). */
class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightDao,
) : WeightRepository {

    override fun observeAll(): Flow<List<Weight>> = dao.observeAll()

    override fun observeById(id: Long): Flow<Weight?> = dao.observeById(id)

    override suspend fun add(weight: Weight): Long = dao.insert(weight)

    override suspend fun hasMeasurements(): Boolean = dao.hasMeasurements()

    override suspend fun addFirst(weight: Weight) = dao.insertFirst(weight)

    override suspend fun update(weight: Weight) = dao.update(weight)

    override suspend fun delete(weight: Weight) = dao.delete(weight)
}
