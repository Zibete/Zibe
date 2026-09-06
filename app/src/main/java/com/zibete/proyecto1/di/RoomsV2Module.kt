package com.zibete.proyecto1.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.zibete.proyecto1.data.roomsv2.FirebaseRoomsV2Repository
import com.zibete.proyecto1.domain.roomsv2.RoomsV2Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomsV2Module {
    @Provides
    @Singleton
    fun provideRoomsV2Repository(
        database: FirebaseDatabase,
        auth: FirebaseAuth,
        functions: FirebaseFunctions,
    ): RoomsV2Repository = FirebaseRoomsV2Repository(database, auth, functions)
}
