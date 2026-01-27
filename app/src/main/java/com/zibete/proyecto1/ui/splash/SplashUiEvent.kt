package com.zibete.proyecto1.ui.splash

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SplashUiEvent {

    object ShowNoInternetDialog : SplashUiEvent()
    object ShowSessionConflictDialog : SplashUiEvent()
    object NavigatePermission : SplashUiEvent()
    object NavigateOnBoarding : SplashUiEvent()
    data class NavigateAuth(
        val deleteAccount: Boolean = false
    ) : SplashUiEvent()

    data class NavigateMain(
        val uiText: UiText? = null,
        val snackType: ZibeSnackType? = null
    ) : SplashUiEvent()
}