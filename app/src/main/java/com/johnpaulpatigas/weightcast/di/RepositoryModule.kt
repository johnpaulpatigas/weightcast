package com.johnpaulpatigas.weightcast.di

import com.johnpaulpatigas.weightcast.core.preferences.UserPreferencesRepository
import com.johnpaulpatigas.weightcast.core.preferences.UserPreferencesRepositoryImpl
import com.johnpaulpatigas.weightcast.data.repository.GoalRepositoryImpl
import com.johnpaulpatigas.weightcast.data.repository.WeightRepositoryImpl
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeightRepository(
        impl: WeightRepositoryImpl
    ): WeightRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        impl: GoalRepositoryImpl
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}
