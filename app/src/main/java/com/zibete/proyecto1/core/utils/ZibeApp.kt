package com.zibete.proyecto1.core.utils

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.zibete.proyecto1.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZibeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val appCheck = FirebaseAppCheck.getInstance()
        val providerFactory = AppCheckProviderFactoryProvider.get()
        appCheck.installAppCheckProviderFactory(providerFactory)
        Log.d("ZibeApp", "AppCheck initialized (${if (BuildConfig.DEBUG) "debug" else "release"})")
        appCheck.getAppCheckToken(false)
            .addOnSuccessListener { Log.d("ZibeApp", "AppCheck token OK") }
            .addOnFailureListener { Log.w("ZibeApp", "AppCheck token FAIL", it) }

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
