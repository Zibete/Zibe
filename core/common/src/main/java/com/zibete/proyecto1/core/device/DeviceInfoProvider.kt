package com.zibete.proyecto1.core.device

interface DeviceInfoProvider {
    fun getModel(): String
    fun getAppVersion(): String
}
