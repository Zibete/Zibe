package com.zibete.proyecto1.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import com.zibete.proyecto1.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideFirebaseRefsContainer(
        database: FirebaseDatabase,
        storage: FirebaseStorage
    ): FirebaseRefsContainer = FirebaseRefsContainer(database, storage)

    @Provides
    @Named("web_client_id")
    fun provideWebClientId(): String = BuildConfig.WEB_CLIENT_ID
}