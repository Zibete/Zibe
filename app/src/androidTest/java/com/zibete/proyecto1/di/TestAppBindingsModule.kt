package com.zibete.proyecto1.di

import com.zibete.proyecto1.data.*
import com.zibete.proyecto1.domain.session.*
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeLogoutUseCase
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserRepositoryActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserSessionActions
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.testing.TestScenarioStore
import com.zibete.proyecto1.core.utils.AppChecksProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppBindingsModule::class]
)
object TestAppBindingsModule {

    @Provides @Singleton
    fun provideAppChecksProvider(store: TestScenarioStore): AppChecksProvider =
        FakeAppChecksProvider { store.scenario }

    @Provides @Singleton
    fun provideUserSessionProvider(store: TestScenarioStore): UserSessionProvider =
        FakeUserSessionProvider { store.scenario }

    @Provides @Singleton
    fun provideUserSessionActions(): UserSessionActions =
        FakeUserSessionActions()

    @Provides @Singleton
    fun provideUserPreferencesProvider(store: TestScenarioStore): UserPreferencesProvider =
        FakeUserPreferencesProvider { store.scenario }

    @Provides @Singleton
    fun provideUserPreferencesActions(store: TestScenarioStore): UserPreferencesActions =
        FakeUserPreferencesActions { store.scenario }

    @Provides @Singleton
    fun provideSessionBootstrapper(): SessionBootstrapper =
        FakeSessionBootstrapper()

    @Provides @Singleton
    fun provideLogoutUseCase(): LogoutUseCase =
        FakeLogoutUseCase()

    @Provides @Singleton
    fun provideDeleteAccountUseCase(): DeleteAccountUseCase =
        FakeDeleteAccountUseCase()

    @Provides @Singleton
    fun provideUserRepositoryActions(): UserRepositoryActions =
        FakeUserRepositoryActions()

    @Provides @Singleton
    fun provideUserRepositoryProvider(): UserRepositoryProvider =
        FakeUserRepositoryProvider()
}

