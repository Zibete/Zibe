package com.zibete.proyecto1.core.di

import com.zibete.proyecto1.core.ui.SnackBarManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnackBarEntryPoint {
    fun snackBarManager(): SnackBarManager
}
