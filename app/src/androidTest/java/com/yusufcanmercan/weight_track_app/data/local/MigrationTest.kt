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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test altyapısı (ADR-0009). Room'un export ettiği şema dosyalarından (`app/schemas/`)
 * gerçek bir eski sürüm veri tabanı oluşturur, veri ekler, sonra güncel kodla açıp veri duruyor mu bakar.
 *
 * v1 → v2 (ADR-0011): `note` sütunu eklendi. Eski kayıtlar durmalı, notları boş olmalı.
 * Yeni sürüm gelince aynı kalıp: version'ı artır, Migration yaz, buraya test ekle.
 */
@RunWith(AndroidJUnit4::class)
// DİKKAT: bu test uygulamanın gerçek veri tabanı dosyasını (`weight_database`) kullanır ve
// bittiğinde uygulama cihazdan kaldırılır; cihazdaki/emülatördeki tüm kayıtlar silinir.
// Değerli verisi olan cihazda çalıştırma.
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WeightDatabase::class.java,
    )

    @Test
    fun v1Verisi_v2ye_gecince_korunur() = runBlocking {
        // 2.3.0 ile aynı şema (v1): bu sürümle açıp 2 kayıt ekle.
        helper.createDatabase(WeightDatabase.NAME, 1).apply {
            execSQL("INSERT INTO Weight (weight, timeStamp) VALUES (80.5, 1000)")
            execSQL("INSERT INTO Weight (weight, timeStamp) VALUES (79.0, 2000)")
            close()
        }

        // Aynı dosyayı uygulamanın gerçek veri tabanı sınıfıyla aç (v2'ye migration çalışır).
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, WeightDatabase::class.java, WeightDatabase.NAME)
            .addMigrations(WeightDatabase.MIGRATION_1_2)
            .build()
        try {
            val weights = db.weightDao().observeAll().first()
            assertEquals(2, weights.size)
            assertEquals(80.5, weights.first { it.timeStamp == 1000L }.weight, 0.0)
            assertNull(weights.first { it.timeStamp == 1000L }.note)
        } finally {
            db.close()
        }
    }

    @Test
    fun sema_dogrulanir() {
        // Dosyadaki şema, şema dosyasındaki tanımla birebir uyuşuyor mu (Room doğrular).
        helper.createDatabase(WeightDatabase.NAME, 1).close()
        helper.runMigrationsAndValidate(WeightDatabase.NAME, 2, true, WeightDatabase.MIGRATION_1_2).close()
    }
}
