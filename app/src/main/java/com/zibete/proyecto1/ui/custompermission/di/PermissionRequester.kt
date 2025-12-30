package com.zibete.proyecto1.ui.custompermission.di

interface PermissionRequester {
    fun requestLocationPermission(onResult: (Boolean) -> Unit)
}