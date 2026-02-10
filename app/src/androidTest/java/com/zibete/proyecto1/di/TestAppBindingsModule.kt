package com.zibete.proyecto1.di

import com.zibete.proyecto1.core.device.DeviceInfoProvider
import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.domain.profile.SendFeedbackUseCase
import com.zibete.proyecto1.domain.profile.UpdateEmailUseCase
import com.zibete.proyecto1.domain.profile.UpdatePasswordUseCase
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.domain.session.SessionConflictMonitor
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeAuthSessionActions
import com.zibete.proyecto1.fakes.FakeAuthSessionProvider
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeExitGroupUseCase
import com.zibete.proyecto1.fakes.FakeGoogleSignInUseCase
import com.zibete.proyecto1.fakes.FakeLogoutUseCase
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUpdateProfileUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserRepositoryActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.testing.TestScenarioStore
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppBindingsModule::class]
)
object TestAppBindingsModule {

    @Provides
    @Singleton
    fun provideAppChecksProvider(store: TestScenarioStore): AppChecksProvider =
        FakeAppChecksProvider { store.scenario }

    @Provides
    @Singleton
    fun provideUserPreferencesProvider(store: TestScenarioStore): UserPreferencesProvider =
        FakeUserPreferencesProvider { store.scenario }

    @Provides
    @Singleton
    fun provideUserPreferencesActions(store: TestScenarioStore): UserPreferencesActions =
        FakeUserPreferencesActions { store.scenario }

    @Provides
    @Singleton
    fun provideSessionBootstrapper(store: TestScenarioStore): SessionBootstrapper =
        FakeSessionBootstrapper { store.scenario }

    @Provides
    @Singleton
    fun provideLogoutUseCase(store: TestScenarioStore): LogoutUseCase =
        FakeLogoutUseCase { store.scenario }

    @Provides
    @Singleton
    fun provideSessionConflictMonitor(): SessionConflictMonitor = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideSessionRepositoryProvider(): SessionRepositoryProvider = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideExitGroupUseCase(store: TestScenarioStore): ExitGroupUseCase =
        FakeExitGroupUseCase { store.scenario }

    @Provides
    @Singleton
    fun provideDeleteAccountUseCase(store: TestScenarioStore): DeleteAccountUseCase =
        FakeDeleteAccountUseCase { store.scenario }

    @Provides
    @Singleton
    fun provideUserRepositoryActions(store: TestScenarioStore): UserRepositoryActions =
        FakeUserRepositoryActions { store.scenario }

    @Provides
    @Singleton
    fun provideUserRepositoryProvider(store: TestScenarioStore): UserRepositoryProvider =
        FakeUserRepositoryProvider { store.scenario }

    @Provides
    @Singleton
    fun provideProfileRepositoryProvider(): ProfileRepositoryProvider =
        mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideProfileRepositoryActions(): ProfileRepositoryActions =
        mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideLocalRepositoryProvider(store: TestScenarioStore): LocalRepositoryProvider {
        return object : LocalRepositoryProvider {
            override val myUserName: String = "Test User"
            override val myProfilePhotoUrl: String = ""
            override val myEmail: String = "test@example.com"
        }
    }

    @Provides
    @Singleton
    fun provideGroupRepositoryProvider(): GroupRepositoryProvider =
        mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideUpdateProfileUseCase(): UpdateProfileUseCase =
        FakeUpdateProfileUseCase()

    @Provides
    @Singleton
    fun provideGoogleSignInUseCase(): GoogleSignInUseCase =
        FakeGoogleSignInUseCase()

    @Provides
    @Singleton
    fun provideAuthSessionActions(store: TestScenarioStore): AuthSessionActions =
        FakeAuthSessionActions { store.scenario }

    @Provides
    @Singleton
    fun provideAuthSessionProvider(store: TestScenarioStore): AuthSessionProvider =
        FakeAuthSessionProvider { store.scenario }

    @Provides
    @Singleton
    fun provideDeviceInfoProvider(): DeviceInfoProvider = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideUpdateEmailUseCase(): UpdateEmailUseCase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideUpdatePasswordUseCase(): UpdatePasswordUseCase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideSendFeedbackUseCase(): SendFeedbackUseCase = mockk(relaxed = true)

}
