package com.zibete.proyecto1.ui.custompermission.di

interface PermissionInteractor {
    fun shouldShowRationale(): Boolean
    fun requestPermission(onResult: (Boolean) -> Unit)
}