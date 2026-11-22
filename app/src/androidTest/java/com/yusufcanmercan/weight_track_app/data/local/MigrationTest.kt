package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test altyapısı (ADR-0009). Room'un export ettiği şema dosyalarından (`app/schemas/`)
 * gerçek bir eski sürüm veri tabanı oluşturur, veri ekler, sonra güncel kodla açıp veri duruyor mu bakar.
 *
 * v1 → v3 tartıyı korur, v2 → v3 mevcut notu koruyup Seyir alanlarını ekler.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WeightDatabase::class.java,
    )

    @Test
    fun v1Verisi_v3e_gecince_korunur() = runBlocking {
        // 2.3.0 ile aynı şema (v1): bu sürümle açıp 2 kayıt ekle.
        val name = "migration-from-v1"
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO Weight (weight, timeStamp) VALUES (80.5, 1000)")
            execSQL("INSERT INTO Weight (weight, timeStamp) VALUES (79.0, 2000)")
            close()
        }

        // Aynı dosyayı uygulamanın Room sınıfıyla açarak iki geçişi birlikte çalıştır.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, WeightDatabase::class.java, name)
            .addMigrations(WeightDatabase.MIGRATION_1_2, WeightDatabase.MIGRATION_2_3)
            .build()
        try {
            val weights = db.weightDao().observeAll().first()
            assertEquals(2, weights.size)
            assertEquals(80.5, weights.first { it.timeStamp == 1000L }.weight, 0.0)
            assertNull(weights.first { it.timeStamp == 1000L }.note)
            assertNull(weights.first { it.timeStamp == 1000L }.mood)
            assertFalse(weights.first { it.timeStamp == 1000L }.hasMeasurementTime)
        } finally {
            db.close()
        }
    }

    @Test
    fun sema_dogrulanir() {
        // Dosyadaki şema, şema dosyasındaki tanımla birebir uyuşuyor mu (Room doğrular).
        val name = "migration-from-v2"
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO Weight (weight, timeStamp, note) VALUES (79.0, 2000, 'Sabah')")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, WeightDatabase.MIGRATION_2_3).use { database ->
            database.query("SELECT note, mood, hasMeasurementTime FROM Weight").use {
                check(it.moveToFirst())
                assertEquals("Sabah", it.getString(0))
                assertNull(it.getString(1))
                assertEquals(0, it.getInt(2))
            }
        }
    }
}
