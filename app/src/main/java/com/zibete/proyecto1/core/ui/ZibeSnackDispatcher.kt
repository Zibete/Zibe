package com.zibete.proyecto1.core.ui

import android.content.Context
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.EntryPointAccessors

object ZibeSnackDispatcher {

    fun show(
        context: Context,
        uiText: UiText,
        type: ZibeSnackType
    ) {
        val appContext = context.applicationContext
        val manager = EntryPointAccessors.fromApplication(
            appContext,
            SnackBarManagerEntryPoint::class.java
        ).snackBarManager()
        manager.show(uiText = uiText, type = type)
    }

    fun show(
        context: Context,
        message: String,
        type: ZibeSnackType
    ) {
        show(context, UiText.Dynamic(message), type)
    }
}
