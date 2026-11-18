package com.yusufcanmercan.weight_track_app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test altyapısı (ADR-0009). Room'un export ettiği şema dosyalarından (`app/schemas/`)
 * gerçek bir eski sürüm veri tabanı oluşturur, veri ekler, sonra güncel kodla açıp veri duruyor mu bakar.
 *
 * Şu an tek sürüm (1) var, migration yok. Şema değişince: version'ı artır, Migration yaz,
 * aşağıdaki iki teste `MIGRATION_1_2` gibi ekle ve v1 verisinin v2'de durduğunu doğrula.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WeightDatabase::class.java,
    )

    @Test
    fun v1Verisi_guncelKodla_acilinca_korunur() = runBlocking {
        // 2.3.0 ile aynı şema (v1): bu sürümle açıp 2 kayıt ekle.
        helper.createDatabase(WeightDatabase.NAME, 1).apply {
            execSQL("INSERT INTO Weight (weight, timeStamp) VALUES (80.5, 1000)")
            execSQL("INSERT INTO Weight (weight, timeStamp) VALUES (79.0, 2000)")
            close()
        }

        // Aynı dosyayı uygulamanın gerçek veri tabanı sınıfıyla aç.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, WeightDatabase::class.java, WeightDatabase.NAME).build()
        try {
            val weights = db.weightDao().observeAll().first()
            assertEquals(2, weights.size)
            assertEquals(80.5, weights.first { it.timeStamp == 1000L }.weight, 0.0)
        } finally {
            db.close()
        }
    }

    @Test
    fun sema_dogrulanir() {
        // Dosyadaki şema, şema dosyasındaki tanımla birebir uyuşuyor mu (Room doğrular).
        helper.createDatabase(WeightDatabase.NAME, 1).close()
        helper.runMigrationsAndValidate(WeightDatabase.NAME, 1, true).close()
    }
}
