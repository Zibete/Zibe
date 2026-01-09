package com.zibete.proyecto1.ui.auth

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class AuthUiEvent {

    data class ShowSnack(
        val message: UiText,
        val type: ZibeSnackType
    ) : AuthUiEvent()

    object NavigateToSplash : AuthUiEvent()
    object NavigateToSignUp : AuthUiEvent()

}
