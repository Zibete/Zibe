package com.zibete.proyecto1.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.zibete.proyecto1.data.roomsv2.FirebaseRoomsV2ProfileRepository
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomsV2ProfileModule {

    @Provides
    @Singleton
    fun provideFirebaseRoomsV2ProfileRepository(
        auth: FirebaseAuth,
        functions: FirebaseFunctions,
    ): FirebaseRoomsV2ProfileRepository =
        FirebaseRoomsV2ProfileRepository(auth, functions)

    @Provides
    @Singleton
    fun provideRoomsV2ProfileRepository(
        repository: FirebaseRoomsV2ProfileRepository,
    ): RoomsV2ProfileRepository = repository
}
