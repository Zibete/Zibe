package com.zibete.proyecto1.ui.chat
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class ChatUiEvent {

    data class ShowSnackbar(
        val message: String,
        val type: ZibeSnackType
    ) : ChatUiEvent()

    //--------------------------------------------------

//    data class ConfirmDeleteChat(val name: String) : ChatUiEvent()


    data class OnDeleteChatConfirmed(val deleteMessages: Boolean) : ChatUiEvent()

//    data class ConfirmDeleteChat(val name: String, val onConfirm: (deleteMessages: Boolean) -> Unit) : ChatUiEvent()


    data object ShowChatDeleted : ChatUiEvent()


    data class AudioUploadSuccess(val duration: String) : ChatUiEvent()
}
