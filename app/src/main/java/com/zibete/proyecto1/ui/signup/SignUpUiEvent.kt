package com.zibete.proyecto1.ui.signup

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SignUpUiEvent {

    data class ShowSnack(
        val message: String,
        val type: ZibeSnackType
    ) : SignUpUiEvent()

    object NavigateToSplash : SignUpUiEvent()
}
