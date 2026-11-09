package com.johnpaulpatigas.weightcast.di

import android.content.Context
import androidx.room.Room
import com.johnpaulpatigas.weightcast.data.local.WeightCastDatabase
import com.johnpaulpatigas.weightcast.data.local.dao.WeightEntryDao
import com.johnpaulpatigas.weightcast.data.local.dao.WeightGoalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWeightCastDatabase(
        @ApplicationContext context: Context
    ): WeightCastDatabase {
        return Room.databaseBuilder(
            context,
            WeightCastDatabase::class.java,
            "weightcast_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideWeightEntryDao(database: WeightCastDatabase): WeightEntryDao {
        return database.weightEntryDao()
    }

    @Provides
    fun provideWeightGoalDao(database: WeightCastDatabase): WeightGoalDao {
        return database.weightGoalDao()
    }
}
