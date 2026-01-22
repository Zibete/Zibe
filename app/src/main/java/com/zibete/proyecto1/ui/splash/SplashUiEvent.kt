package com.zibete.proyecto1.ui.splash

sealed class SplashUiEvent {

    object ShowNoInternetDialog : SplashUiEvent()
    object ShowSessionConflictDialog : SplashUiEvent()
    object NavigatePermission : SplashUiEvent()
    object NavigateOnBoarding : SplashUiEvent()
    object NavigateAuth : SplashUiEvent()
    object NavigateMain : SplashUiEvent()
}