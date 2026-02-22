package com.zibete.proyecto1.core.utils

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.zibete.proyecto1.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import java.net.ProxySelector

@HiltAndroidApp
class ZibeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val appCheck = FirebaseAppCheck.getInstance()
        val providerFactory: AppCheckProviderFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            val factoryClass =
                Class.forName("com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory")
            factoryClass.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
        }
        appCheck.installAppCheckProviderFactory(providerFactory)
        Log.d("ZibeApp", "AppCheck initialized (${if (BuildConfig.DEBUG) "debug" else "release"})")
        appCheck.getAppCheckToken(false)

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
