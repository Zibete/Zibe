package com.zibete.proyecto1.ui.editprofile

sealed interface EditProfileUiEvent {

    data object NavigateBack : EditProfileUiEvent
    data object NavigateToSettings : EditProfileUiEvent
}
