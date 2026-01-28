package com.zibete.proyecto1.di

import com.zibete.proyecto1.core.di.ValidationModule
import com.zibete.proyecto1.core.validation.EmailValidator
import com.zibete.proyecto1.fakes.FakeEmailValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ValidationModule::class]
)
object FakeValidationModule {

    @Provides
    @Singleton
    fun provideEmailValidator(): EmailValidator =
        FakeEmailValidator(result = true)
}
