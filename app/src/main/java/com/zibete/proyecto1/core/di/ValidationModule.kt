package com.zibete.proyecto1.core.di

import com.zibete.proyecto1.core.validation.AndroidEmailValidator
import com.zibete.proyecto1.core.validation.EmailValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ValidationModule {

    @Provides
    @Singleton
    fun provideEmailValidator(): EmailValidator =
        AndroidEmailValidator()
}
