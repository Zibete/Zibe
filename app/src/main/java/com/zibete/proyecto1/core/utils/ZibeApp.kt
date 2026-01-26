package com.zibete.proyecto1.core.utils

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import java.net.ProxySelector

@HiltAndroidApp
class ZibeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Fix for java.lang.SecurityException: No permission to access APN settings
        // This prevents SDKs from trying to access restricted telephony providers
        // for proxy settings on older Android versions.
        val defaultSelector = ProxySelector.getDefault()
        if (defaultSelector != null) {
            ProxySelector.setDefault(defaultSelector)
        }

        ScreenUtils.init(this)
    }

    object ScreenUtils {
        var widthPx: Int = 0
        var heightPx: Int = 0
        var density: Float = 0f

        fun init(context: Context) {
            val dm = context.resources.displayMetrics
            widthPx = dm.widthPixels
            heightPx = dm.heightPixels
            density = dm.density
        }
    }
}
