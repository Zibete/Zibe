package com.zibete.proyecto1.di

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.BuildConfig
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance(checkedFirebaseApp())

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance(checkedFirebaseApp())

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance(checkedFirebaseApp())

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance(checkedFirebaseApp())

    private fun checkedFirebaseApp(): FirebaseApp = FirebaseApp.getInstance().also { app ->
        check(!BuildConfig.IS_LOCAL_BACKEND || app.options.projectId == "demo-zibe-rooms") {
            "The local build requires a demo Firebase application initialized before Hilt"
        }
    }

    @Provides
    @Named("web_client_id")
    fun provideWebClientId(): String = BuildConfig.WEB_CLIENT_ID

    @Provides
    @Singleton
    fun provideFirebaseRefsContainer(
        database: FirebaseDatabase,
        storage: FirebaseStorage
    ): FirebaseRefsContainer = FirebaseRefsContainer(database, storage)
}
