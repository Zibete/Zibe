package com.zibete.proyecto1.ui.splash

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SplashUiEvent {

    // Sin internet → mostrar diálogo con "Reintentar"
    object ShowNoInternetDialog : SplashUiEvent()

    data class ShowTokenDialog(
        val mail: String,
        val flag: Int
    ) : SplashUiEvent()

    data class ShowSnackbar(
        val message: String,
        val type: ZibeSnackType
    ) : SplashUiEvent()

    object NavigateAuth : SplashUiEvent()
    object NavigateEditProfile : SplashUiEvent()
    object NavigateMain : SplashUiEvent()
    object RequestLocationPermission : SplashUiEvent()
    object NavigateOnBoarding : SplashUiEvent()

}
