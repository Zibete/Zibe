package com.zibete.proyecto1.ui.splash

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SplashUiEvent {

    data class ShowSnack(
        val message: UiText,
        val type: ZibeSnackType
    ) : SplashUiEvent()

    object ShowNoInternetDialog : SplashUiEvent()
    object ShowSessionConflictDialog : SplashUiEvent()
    object NavigatePermission : SplashUiEvent()
    object NavigateOnBoarding : SplashUiEvent()
    object NavigateAuth : SplashUiEvent()
    object NavigateMain : SplashUiEvent()
}