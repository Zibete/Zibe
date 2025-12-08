package com.zibete.proyecto1.ui.chat
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class ChatUiEvent {

    data class ShowSnackbar(
        val message: String,
        val type: ZibeSnackType
    ) : ChatUiEvent()

    //--------------------------------------------------

    data class OnDeleteChatConfirmed(val deleteMessages: Boolean) : ChatUiEvent()



    data object ShowChatDeleted : ChatUiEvent()


    data class AudioUploadSuccess(val duration: String) : ChatUiEvent()
}
