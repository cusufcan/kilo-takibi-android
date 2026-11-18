package com.yusufcanmercan.weight_track_app.data.repository

import com.yusufcanmercan.weight_track_app.data.local.Weight
import kotlinx.coroutines.flow.Flow

/**
 * Kilo kayıtlarının tek giriş kapısı. ViewModel yalnız bu arayüzü tanır; verinin Room'dan
 * (ileride bulutla birlikte) geldiğini bilmez. Testte yerine sahtesi verilebilir.
 */
interface WeightRepository {
    /** Tarihe göre sıralı tüm kayıtlar; veri değişince yeni liste gelir. */
    fun observeAll(): Flow<List<Weight>>

    /** Tek kayıt; yoksa `null`. */
    fun observeById(id: Long): Flow<Weight?>

    suspend fun add(weight: Weight)

    suspend fun update(weight: Weight)

    suspend fun delete(weight: Weight)
}
