package com.zibete.proyecto1.di

import com.zibete.proyecto1.core.device.DefaultDeviceInfoProvider
import com.zibete.proyecto1.core.device.DeviceInfoProvider
import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.core.utils.DefaultAppChecksProvider
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.DefaultGoogleSignInUseCase
import com.zibete.proyecto1.data.auth.FirebaseSessionManager
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.data.profile.ProfileRepository
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.domain.profile.DefaultSendFeedbackUseCase
import com.zibete.proyecto1.domain.profile.DefaultUpdateEmailUseCase
import com.zibete.proyecto1.domain.profile.DefaultUpdatePasswordUseCase
import com.zibete.proyecto1.domain.profile.DefaultUpdateProfileUseCase
import com.zibete.proyecto1.domain.profile.SendFeedbackUseCase
import com.zibete.proyecto1.domain.profile.UpdateEmailUseCase
import com.zibete.proyecto1.domain.profile.UpdatePasswordUseCase
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.domain.session.DefaultDeleteAccountUseCase
import com.zibete.proyecto1.domain.session.DefaultExitGroupUseCase
import com.zibete.proyecto1.domain.session.DefaultLogoutUseCase
import com.zibete.proyecto1.domain.session.DefaultSessionBootstrapper
import com.zibete.proyecto1.domain.session.DefaultSessionConflictMonitor
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.domain.session.SessionConflictMonitor
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAppChecksProvider(
        impl: DefaultAppChecksProvider
    ): AppChecksProvider

    @Binds
    abstract fun bindAuthSessionActions(
        impl: FirebaseSessionManager
    ): AuthSessionActions

    @Binds
    abstract fun bindAuthSessionProvider(
        impl: FirebaseSessionManager
    ): AuthSessionProvider

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(
        impl: DefaultDeviceInfoProvider
    ): DeviceInfoProvider

    @Binds
    abstract fun bindLocalRepositoryProvider(
        impl: UserRepository
    ): LocalRepositoryProvider

    @Binds
    @Singleton
    abstract fun bindProfileRepositoryProvider(
        impl: ProfileRepository
    ): ProfileRepositoryProvider

    @Binds
    @Singleton
    abstract fun bindProfileRepositoryActions(
        impl: ProfileRepository
    ): ProfileRepositoryActions

    @Binds
    @Singleton
    abstract fun bindSessionRepositoryActions(
        impl: SessionRepository
    ): SessionRepositoryActions

    @Binds
    @Singleton
    abstract fun bindSessionRepositoryProvider(
        impl: SessionRepository
    ): SessionRepositoryProvider

    @Binds
    @Singleton
    abstract fun bindUserPreferencesActions(
        impl: UserPreferencesRepository
    ): UserPreferencesActions

    @Binds
    @Singleton
    abstract fun bindUserPreferencesProvider(
        impl: UserPreferencesRepository
    ): UserPreferencesProvider

    @Binds
    @Singleton
    abstract fun bindUserRepositoryActions(
        impl: UserRepository
    ): UserRepositoryActions

    @Binds
    @Singleton
    abstract fun bindUserRepositoryProvider(
        impl: UserRepository
    ): UserRepositoryProvider

    @Binds
    @Singleton
    abstract fun bindDeleteAccountUseCase(
        impl: DefaultDeleteAccountUseCase
    ): DeleteAccountUseCase

    @Binds
    abstract fun bindExitGroupUseCase(
        impl: DefaultExitGroupUseCase
    ): ExitGroupUseCase

    @Binds
    abstract fun bindGroupRepositoryProvider(
        impl: GroupRepository
    ): GroupRepositoryProvider

    @Binds
    @Singleton
    abstract fun bindGoogleSignInUseCase(
        impl: DefaultGoogleSignInUseCase
    ): GoogleSignInUseCase

    @Binds
    @Singleton
    abstract fun bindLogoutUseCase(
        impl: DefaultLogoutUseCase
    ): LogoutUseCase

    @Binds
    abstract fun bindSendFeedbackUseCase(
        impl: DefaultSendFeedbackUseCase
    ): SendFeedbackUseCase

    @Binds
    abstract fun bindUpdateEmailUseCase(
        impl: DefaultUpdateEmailUseCase
    ): UpdateEmailUseCase

    @Binds
    abstract fun bindUpdatePasswordUseCase(
        impl: DefaultUpdatePasswordUseCase
    ): UpdatePasswordUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateProfileUseCase(
        impl: DefaultUpdateProfileUseCase
    ): UpdateProfileUseCase

    // --- Domain orchestrators ---
    @Binds
    @Singleton
    abstract fun bindSessionBootstrapper(
        impl: DefaultSessionBootstrapper
    ): SessionBootstrapper

    @Binds
    @Singleton
    abstract fun bindSessionConflictMonitor(
        impl: DefaultSessionConflictMonitor
    ): SessionConflictMonitor
}
