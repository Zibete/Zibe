package com.zibete.proyecto1.core.utils

import com.google.firebase.appcheck.FirebaseAppCheck
import com.zibete.proyecto1.local.LocalFirebaseBackend

object AppCheckProviderFactoryProvider {
    fun initialize() {
        LocalFirebaseBackend.requireInitialized()
        FirebaseAppCheck.getInstance().setTokenAutoRefreshEnabled(false)
    }
}
