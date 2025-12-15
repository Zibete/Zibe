package com.zibete.proyecto1.ui.editprofile

sealed interface EditProfileUiEvent {
    data class ShowMessage(val message: String) : EditProfileUiEvent
}
