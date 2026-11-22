package com.yusufcanmercan.weight_track_app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.yusufcanmercan.weight_track_app.data.local.WeightDao
import com.yusufcanmercan.weight_track_app.data.local.WeightDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.File
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Her test kendi bellek içi veritabanıyla başlar; gerçek kayıtlara dokunulmaz. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeightDatabase =
        Room.inMemoryDatabaseBuilder(context, WeightDatabase::class.java).build()

    @Provides
    fun provideWeightDao(database: WeightDatabase): WeightDao = database.weightDao()
}

/** Her test boş ayarlarla başlar: dosya adı test başına yenidir. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreModule::class])
object TestDataStoreModule {
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            File(context.cacheDir, "test-settings-${UUID.randomUUID()}.preferences_pb")
        }
}
