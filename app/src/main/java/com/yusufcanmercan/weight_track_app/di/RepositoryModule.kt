package com.yusufcanmercan.weight_track_app.di

import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepositoryImpl
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** "WeightRepository istenirse WeightRepositoryImpl ver" der. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
