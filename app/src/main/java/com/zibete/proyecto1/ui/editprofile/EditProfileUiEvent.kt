package com.zibete.proyecto1.ui.editprofile

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface EditProfileUiEvent {

    data class ShowMessage(
        val message: String,
        val type: ZibeSnackType? = ZibeSnackType.INFO
    ) : EditProfileUiEvent

    data class OnBackToMain(
        val message: String
    ) : EditProfileUiEvent

}
