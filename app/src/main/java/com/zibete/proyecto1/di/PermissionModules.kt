package com.zibete.proyecto1.di

import android.app.Activity
import com.zibete.proyecto1.ui.custompermission.di.ActivityResultPermissionRequester
import com.zibete.proyecto1.ui.custompermission.di.AndroidPermissionInteractor
import com.zibete.proyecto1.ui.custompermission.di.AndroidRationaleProvider
import com.zibete.proyecto1.ui.custompermission.di.PermissionInteractor
import com.zibete.proyecto1.ui.custompermission.di.PermissionRequester
import com.zibete.proyecto1.ui.custompermission.di.RationaleProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
object RationaleModule {
    @Provides
    fun provideRationaleProvider(activity: Activity): RationaleProvider =
        AndroidRationaleProvider(activity)
}

@Module
@InstallIn(SingletonComponent::class)
object PermissionRequesterModule {

    @Provides
    @Singleton
    fun providePermissionRequester(): ActivityResultPermissionRequester {
        return ActivityResultPermissionRequester()
    }

    @Provides
    fun bindAsInterface(impl: ActivityResultPermissionRequester): PermissionRequester = impl
}

@Module
@InstallIn(ActivityComponent::class)
object PermissionModule {

    @Provides
    fun providePermissionInteractor(activity: Activity): PermissionInteractor {
        return AndroidPermissionInteractor(activity)
    }
}