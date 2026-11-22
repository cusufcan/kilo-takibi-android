package com.yusufcanmercan.weight_track_app.di

import android.content.Context
import androidx.room.Room
import com.yusufcanmercan.weight_track_app.data.local.WeightDao
import com.yusufcanmercan.weight_track_app.data.local.WeightDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Veri tabanı ve DAO'yu Hilt'e sağlar. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeightDatabase =
        Room.databaseBuilder(context, WeightDatabase::class.java, WeightDatabase.NAME)
            .addMigrations(WeightDatabase.MIGRATION_1_2, WeightDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideWeightDao(database: WeightDatabase): WeightDao = database.weightDao()
}
