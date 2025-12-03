package com.zibete.proyecto1.ui.chat
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class ChatUiEvent {

    data class ShowSnackbar(
        val message: String,
        val type: ZibeSnackType
    ) : ChatUiEvent()

    //--------------------------------------------------

//    data class ConfirmDeleteChat(val name: String) : ChatUiEvent()
//    data class ConfirmUnblock(val name: String) : ChatUiEvent()
//    data class ConfirmBlock(val name: String) : ChatUiEvent()

    data class OnDeleteChatConfirmed(val deleteMessages: Boolean) : ChatUiEvent()

    object OnUnblockConfirmed : ChatUiEvent()

    object OnBlockConfirmed : ChatUiEvent()

//    data class ConfirmDeleteChat(val name: String, val onConfirm: (deleteMessages: Boolean) -> Unit) : ChatUiEvent()
    data object ShowChatDeleted : ChatUiEvent()

    //--------------------------------------------------
//    data class ConfirmUnblock(val name: String, val onConfirm: () -> Unit) : ChatUiEvent()
    //--------------------------------------------------
    data class ShowBlockSuccess(val name: String) : ChatUiEvent()
    data class ShowUnblockSuccess(val name: String) : ChatUiEvent()
    data class ShowToggleNotificationSuccess(val name: String, val enabled: Boolean) : ChatUiEvent()
    //--------------------------------------------------
    data class NotifyUiUpdate(val type: String) : ChatUiEvent()
    data class AudioUploadSuccess(val duration: String) : ChatUiEvent()
}
