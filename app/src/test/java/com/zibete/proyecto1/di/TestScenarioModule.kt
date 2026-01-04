package com.zibete.proyecto1.di

import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.UnitScenarioStore
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestScenarioModule {

    @Provides
    @Singleton
    fun provideScenarioProvider(store: UnitScenarioStore): () -> TestScenario =
        { store.scenario }
}
