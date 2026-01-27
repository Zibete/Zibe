package com.zibete.proyecto1.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

data class SettingsConfig(
    val navigationDelay: Long = 450L,
    val validationDebounce: Long = 800L
)

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideSettingsConfig(): SettingsConfig {
        return SettingsConfig()
    }
}
