package com.zibete.proyecto1.ui.splash

import android.content.Intent
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SplashUiEvent {

    // Sin internet → mostrar diálogo con "Reintentar"
    object ShowNoInternetDialog : SplashUiEvent()
    object ShowSessionConflictDialog : SplashUiEvent()
    object NavigateAuth : SplashUiEvent()
    class Navigate(
        val intent : Intent
    ) : SplashUiEvent()
    object NavigateMain : SplashUiEvent()
    object RequestLocationPermission : SplashUiEvent()
    object NavigateOnBoarding : SplashUiEvent()

    data class ShowSnackbar(
        val message: String,
        val type: ZibeSnackType
    ) : SplashUiEvent()

}
