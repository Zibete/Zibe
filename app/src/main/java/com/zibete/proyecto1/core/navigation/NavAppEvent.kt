package com.zibete.proyecto1.core.navigation

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface NavAppEvent {
    data class FinishFlowNavigateToSplash(
        val snackMessage: UiText? = null,
        val snackType: ZibeSnackType? = null,
        val deleteAccount: Boolean = false,
        val sessionConflict: Boolean = false
    ) : NavAppEvent
}
