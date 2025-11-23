package com.zibete.proyecto1.ui.splash

sealed class SplashUiEvent {

    object ShowNoInternet : SplashUiEvent()

    data class ShowTokenDialog(
        val mail: String,
        val flag: Int
    ) : SplashUiEvent()

    object NavigateAuth : SplashUiEvent()
    object NavigateEditProfile : SplashUiEvent()
    object NavigateMain : SplashUiEvent()

    object RequestLocationPermission : SplashUiEvent()
}
