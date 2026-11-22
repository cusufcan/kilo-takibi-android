package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Veri tabanına sorular. Okuyanlar `Flow` döndürür: veri değişince ekrana yeni liste kendiliğinden gelir. */
@Dao
interface WeightDao {

    @Query("SELECT * FROM Weight ORDER BY timeStamp")
    fun observeAll(): Flow<List<Weight>>

    @Query("SELECT * FROM Weight WHERE id = :id")
    fun observeById(id: Long): Flow<Weight?>

    /** Eklenen satırın yeni `id`'sini döndürür. */
    @Insert
    suspend fun insert(weight: Weight): Long

    @Query("SELECT EXISTS(SELECT 1 FROM Weight)")
    suspend fun hasMeasurements(): Boolean

    @Transaction
    suspend fun insertFirst(weight: Weight) {
        if (!hasMeasurements()) insert(weight)
    }

    @Update
    suspend fun update(weight: Weight)

    @Delete
    suspend fun delete(weight: Weight)
}
