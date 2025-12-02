package com.zibete.proyecto1.ui.users


sealed class UsersUiEvent {
    object ShowFilterDialog : UsersUiEvent()
    data class ShowError(val message: String) : UsersUiEvent()
    object ScrollToBottom : UsersUiEvent()
}


