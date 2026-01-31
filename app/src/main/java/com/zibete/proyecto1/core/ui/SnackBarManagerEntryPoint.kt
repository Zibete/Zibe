package com.zibete.proyecto1.core.ui

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnackBarManagerEntryPoint {
    fun snackBarManager(): SnackBarManager
}
