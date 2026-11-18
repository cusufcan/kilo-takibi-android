package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Veri tabanı. v1 = eski 2.3.0 şeması (ADR-0002); v2 = `note` sütunu (ADR-0011).
 * Yeni sütun gerekince version artar ve elle yazılmış Migration eklenir;
 * `fallbackToDestructiveMigration()` asla kullanılmaz.
 */
@Database(entities = [Weight::class], version = 2, exportSchema = true)
abstract class WeightDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    companion object {
        /** Eski sürümdeki dosya adı; değişirse eski veri bulunamaz. */
        const val NAME = "weight_database"

        /** v1 → v2: `note` sütunu eklenir. Mevcut satırlar korunur, notları boş (`NULL`) olur. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Weight ADD COLUMN note TEXT")
            }
        }
    }
}
