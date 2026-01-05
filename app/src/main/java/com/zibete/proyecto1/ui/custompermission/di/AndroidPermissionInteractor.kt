package com.zibete.proyecto1.ui.custompermission.di

import android.Manifest
import android.app.Activity
import androidx.core.app.ActivityCompat
import javax.inject.Inject

class AndroidPermissionInteractor @Inject constructor(
    private val activity: Activity
) : PermissionInteractor {

    override fun shouldShowRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        throw UnsupportedOperationException("Use ActivityResultLauncher in UI for real requests")
    }
}