package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 eski 2.3.0 şeması, v2 not, v3 duygu ve ölçüm saatinin belirli olup olmadığı.
 * Yeni sütun gerekince version artar ve elle yazılmış Migration eklenir;
 * `fallbackToDestructiveMigration()` asla kullanılmaz.
 */
@Database(entities = [Weight::class], version = 3, exportSchema = true)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Weight ADD COLUMN mood TEXT")
                db.execSQL("ALTER TABLE Weight ADD COLUMN hasMeasurementTime INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
