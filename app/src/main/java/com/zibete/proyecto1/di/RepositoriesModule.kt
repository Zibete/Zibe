package com.zibete.proyecto1.di

import android.content.Context
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.PresenceRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoriesModule {

    @Provides
    @Singleton
    fun providePresenceRepository(
        @ApplicationContext context: Context,
        firebaseRefsContainer: FirebaseRefsContainer,
        authSessionProvider: AuthSessionProvider
    ): PresenceRepository =
        PresenceRepository(context, firebaseRefsContainer, authSessionProvider)

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseRefsContainer: FirebaseRefsContainer,
        authSessionProvider: AuthSessionProvider,
        presenceRepository: PresenceRepository,
        @ApplicationContext context: Context
    ): UserRepository =
        UserRepository(firebaseRefsContainer, authSessionProvider, presenceRepository, context)

    @Provides
    @Singleton
    fun provideGroupRepository(
        firebaseRefsContainer: FirebaseRefsContainer,
        authSessionProvider: AuthSessionProvider
    ): GroupRepository =
        GroupRepository(firebaseRefsContainer, authSessionProvider)

    @Provides
    @Singleton
    fun provideLocationRepository(
        firebaseRefsContainer: FirebaseRefsContainer,
        authSessionProvider: AuthSessionProvider
    ): LocationRepository =
        LocationRepository(firebaseRefsContainer, authSessionProvider)
}
