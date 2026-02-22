package com.zibete.proyecto1.core.utils

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckProviderFactoryProvider {
    fun get(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
