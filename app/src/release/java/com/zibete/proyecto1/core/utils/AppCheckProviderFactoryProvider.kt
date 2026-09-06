package com.zibete.proyecto1.core.utils

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckProviderFactoryProvider {
    fun initialize() {
        FirebaseAppCheck.getInstance().apply {
            installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            getAppCheckToken(false)
        }
    }
}
