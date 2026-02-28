package com.zibete.proyecto1.core.device

import android.os.Build
import com.zibete.proyecto1.BuildConfig
import jakarta.inject.Inject

interface DeviceInfoProvider {
    fun getModel(): String
    fun getAppVersion(): String
}

class DefaultDeviceInfoProvider @Inject constructor() : DeviceInfoProvider {
    override fun getModel(): String = Build.MODEL
    override fun getAppVersion(): String = BuildConfig.VERSION_NAME
}