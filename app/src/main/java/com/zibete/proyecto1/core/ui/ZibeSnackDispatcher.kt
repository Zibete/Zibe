package com.zibete.proyecto1.core.ui

import com.zibete.proyecto1.ui.components.ZibeSnackType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZibeSnackDispatcher @Inject constructor(
    private val snackBarManager: SnackBarManager
) {
    fun show(
        uiText: UiText,
        type: ZibeSnackType
    ) {
        snackBarManager.show(uiText = uiText, type = type)
    }
}
