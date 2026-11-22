package com.yusufcanmercan.weight_track_app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Gerçek DataStore, geçici dosyada: Android gerekmeden JVM'de çalışır. */
class SettingsRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newStore(scope: kotlinx.coroutines.CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }

    @Test
    fun `kayit yoksa tema SYSTEM olur`() = runTest {
        val repo = SettingsRepositoryImpl(newStore(backgroundScope))

        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test
    fun `secilen tema yazilir ve geri okunur`() = runTest {
        val repo = SettingsRepositoryImpl(newStore(backgroundScope))

        repo.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repo.themeMode.first())
    }

    @Test
    fun `bozuk kayit cokertmez SYSTEM olur`() = runTest {
        val store = newStore(backgroundScope)
        store.edit { it[stringPreferencesKey("theme_mode")] = "BOZUK" }

        assertEquals(ThemeMode.SYSTEM, SettingsRepositoryImpl(store).themeMode.first())
    }

    @Test
    fun `hedef yoksa null olur`() = runTest {
        assertEquals(null, SettingsRepositoryImpl(newStore(backgroundScope)).goalWeight.first())
    }

    @Test
    fun `hedef yazilir ve null verilince silinir`() = runTest {
        val repo = SettingsRepositoryImpl(newStore(backgroundScope))

        repo.setGoalWeight(72.5)
        assertEquals(72.5, repo.goalWeight.first())

        repo.setGoalWeight(null)
        assertEquals(null, repo.goalWeight.first())
    }

    @Test
    fun `dosya okunamazsa varsayilanlar gelir cokmez`() = runTest {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("disk") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = throw IOException("disk")
        }
        val repo = SettingsRepositoryImpl(broken)

        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        assertEquals(null, repo.goalWeight.first())
        assertEquals(false, repo.onboarding.first().completed)
    }
}
