package com.zibete.proyecto1.utils

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZibeApp : Application() {
    override fun onCreate() {
        super.onCreate()

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
