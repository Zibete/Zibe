package com.zibete.proyecto1.local

import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Compiled into the local app so instrumentation reaches its real Hilt component. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocalBackendProbeEntryPoint {
    fun sessionProvider(): SessionRepositoryProvider
    fun sessionActions(): SessionRepositoryActions
    fun preferencesActions(): UserPreferencesActions
}
