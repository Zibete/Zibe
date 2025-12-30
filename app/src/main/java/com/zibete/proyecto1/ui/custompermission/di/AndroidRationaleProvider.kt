package com.zibete.proyecto1.ui.custompermission.di

import android.Manifest
import android.app.Activity
import androidx.core.app.ActivityCompat
import javax.inject.Inject

class AndroidRationaleProvider @Inject constructor(
    private val activity: Activity
) : RationaleProvider {

    override fun shouldShowRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}