package com.zibete.proyecto1.ui.splash

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SplashUiEvent {

    // Sin internet → mostrar diálogo con "Reintentar"
    object ShowNoInternetDialog : SplashUiEvent()
    object ShowSessionConflictDialog : SplashUiEvent()
    object NavigatePermission : SplashUiEvent()
    object NavigateOnBoarding : SplashUiEvent()
    object NavigateAuth : SplashUiEvent()
    object NavigateMain : SplashUiEvent()

    data class ShowSnack(
        val message: String,
        val type: ZibeSnackType
    ) : SplashUiEvent()
}
