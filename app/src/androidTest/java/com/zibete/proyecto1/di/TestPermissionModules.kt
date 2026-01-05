package com.zibete.proyecto1.di

import com.zibete.proyecto1.testing.TestPermissionConfig
import com.zibete.proyecto1.ui.custompermission.di.PermissionRequester
import com.zibete.proyecto1.ui.custompermission.di.RationaleProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [ActivityComponent::class],
    replaces = [RationaleModule::class],
)
object TestRationaleModule {

    @Provides
    fun provideRationaleProvider(): RationaleProvider =
        object : RationaleProvider {
            override fun shouldShowRationale(): Boolean = TestPermissionConfig.shouldShowRationale
        }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PermissionRequesterModule::class]
)
object TestPermissionRequesterModule {

    @Provides
    fun providePermissionRequester(): PermissionRequester =
        object : PermissionRequester {
            override fun requestLocationPermission(onResult: (Boolean) -> Unit) {
                onResult(TestPermissionConfig.grantResult)
            }
        }
}
