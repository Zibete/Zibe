package com.zibete.proyecto1.ui.custompermission.di

import android.Manifest
import androidx.activity.result.ActivityResultLauncher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityResultPermissionRequester @Inject constructor() : PermissionRequester {

    @Volatile private var launcher: ActivityResultLauncher<String>? = null
    @Volatile private var pendingCallback: ((Boolean) -> Unit)? = null

    fun bindLauncher(launcher: ActivityResultLauncher<String>) {
        this.launcher = launcher
    }

    fun onPermissionResult(isGranted: Boolean) {
        val cb = pendingCallback
        pendingCallback = null
        cb?.invoke(isGranted)
    }

    override fun requestLocationPermission(onResult: (Boolean) -> Unit) {
        pendingCallback = onResult
        val l = launcher ?: error("PermissionRequester launcher not bound. Call bindLauncher() from UI.")
        l.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}