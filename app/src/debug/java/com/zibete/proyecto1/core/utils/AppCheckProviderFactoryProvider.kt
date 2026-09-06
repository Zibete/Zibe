package com.zibete.proyecto1.core.utils

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckProviderFactoryProvider {
    fun initialize() {
        FirebaseAppCheck.getInstance().apply {
            installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            getAppCheckToken(false)
        }
    }
}
