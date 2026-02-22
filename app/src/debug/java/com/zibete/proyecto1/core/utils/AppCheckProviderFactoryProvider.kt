package com.zibete.proyecto1.core.utils

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckProviderFactoryProvider {
    fun get(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
