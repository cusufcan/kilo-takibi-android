package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Veri tabanı. `version = 1`: çekirdek aşamada şema eski sürümle aynı (ADR-0002).
 * Yeni sütun gerekince version artar ve elle yazılmış Migration eklenir;
 * `fallbackToDestructiveMigration()` asla kullanılmaz.
 */
@Database(entities = [Weight::class], version = 1, exportSchema = true)
abstract class WeightDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    companion object {
        /** Eski sürümdeki dosya adı; değişirse eski veri bulunamaz. */
        const val NAME = "weight_database"
    }
}
