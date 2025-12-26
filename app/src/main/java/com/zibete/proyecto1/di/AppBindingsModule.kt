package com.zibete.proyecto1.di


import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.utils.DefaultAppChecksProvider

import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionManager

import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider

import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.domain.session.DefaultSessionBootstrapper

import com.zibete.proyecto1.domain.session.LogoutOrchestrator
import com.zibete.proyecto1.domain.session.DefaultLogoutOrchestrator
import com.zibete.proyecto1.utils.AppChecksProvider

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
    @Singleton
    abstract fun bindUserSessionProvider(
        impl: UserSessionManager
    ): UserSessionProvider

    @Binds
    @Singleton
    abstract fun bindUserSessionActions(
        impl: UserSessionManager
    ): UserSessionActions

    @Binds
    @Singleton
    abstract fun bindUserPreferencesProvider(
        impl: UserPreferencesRepository
    ): UserPreferencesProvider

    @Binds
    @Singleton
    abstract fun bindUserPreferencesActions(
        impl: UserPreferencesRepository
    ): UserPreferencesActions

    // --- Domain orchestrators ---
    @Binds
    @Singleton
    abstract fun bindSessionBootstrapper(
        impl: DefaultSessionBootstrapper
    ): SessionBootstrapper

    @Binds
    @Singleton
    abstract fun bindLogoutOrchestrator(
        impl: DefaultLogoutOrchestrator
    ): LogoutOrchestrator

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
    abstract fun bindUserRepositoryActions(
        impl: UserRepository
    ): UserRepositoryActions

    @Binds
    @Singleton
    abstract fun bindUserRepositoryProvider(
        impl: UserRepository
    ): UserRepositoryProvider
}
