package com.zibete.proyecto1.ui.auth

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class AuthUiEvent {
    data class ShowSnack(
        val message: String,
        val type: ZibeSnackType
    ) : AuthUiEvent()
    object NavigateToSplash : AuthUiEvent()
}
