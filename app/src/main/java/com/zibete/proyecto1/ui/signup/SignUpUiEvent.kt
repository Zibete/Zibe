package com.zibete.proyecto1.ui.signup

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class SignUpUiEvent {

    data class ShowSnack(
        val uiText: UiText,
        val type: ZibeSnackType
    ) : SignUpUiEvent()

    object NavigateToSplash : SignUpUiEvent()
}
