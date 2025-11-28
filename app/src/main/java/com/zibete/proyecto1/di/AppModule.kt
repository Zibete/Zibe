package com.zibete.proyecto1.di

import android.content.Context
import com.facebook.login.LoginManager // No es Firebase, es Facebook
import com.zibete.proyecto1.data.UserPreferencesRepository // Lógica de SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 1. Contexto de Aplicación (Base para todo)
    // Hilt provee el Contexto automáticamente

    // 2. Repositorio de Preferencias (Necesita Contexto para SharedPreferences)


    // 3. Login Manager (Servicio de terceros no relacionado con Firebase)
    @Provides
    @Singleton
    fun provideLoginManager(): LoginManager {
        return LoginManager.getInstance()
    }

    // 4. Otros Servicios/Managers (Ej: NetworkChecker, Analítica)
    // ...
}