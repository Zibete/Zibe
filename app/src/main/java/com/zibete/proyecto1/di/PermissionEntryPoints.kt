package com.zibete.proyecto1.di

import com.zibete.proyecto1.ui.custompermission.di.ActivityResultPermissionRequester
import com.zibete.proyecto1.ui.custompermission.di.RationaleProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PermissionRequesterEntryPoint {
    fun requester(): ActivityResultPermissionRequester
}


@EntryPoint
@InstallIn(ActivityComponent::class)
interface RationaleEntryPoint {
    fun rationaleProvider(): RationaleProvider
}