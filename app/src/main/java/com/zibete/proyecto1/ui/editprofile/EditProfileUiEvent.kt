package com.zibete.proyecto1.ui.editprofile

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface EditProfileUiEvent {

    data class ShowSnack(
        val uiText: UiText,
        val type: ZibeSnackType
    ) : EditProfileUiEvent

    data object OnBackToMain : EditProfileUiEvent
}
