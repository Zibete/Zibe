package com.zibete.proyecto1.di

import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.PresenceRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.testing.fakeLocationRepository
import com.zibete.proyecto1.testing.fakePresenceRepository
import com.zibete.proyecto1.testing.fakeUserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoriesModule::class]
)
object TestRepositoriesModule {

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = fakeUserRepository()

    @Provides
    @Singleton
    fun providePresenceRepository(): PresenceRepository = fakePresenceRepository()

    @Provides
    @Singleton
    fun provideLocationRepository(): LocationRepository = fakeLocationRepository()
}
