package com.zibete.proyecto1.ui.chat.session

sealed class ChatSessionUiEvent {

    // ------- HIDE CHAT (solo lo usarás en la lista) -------
    data class ConfirmHideChat(
        val name: String,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    // ------- BLOCK / UNBLOCK -------
    data class ConfirmBlock(
        val name: String,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    data class ConfirmUnblock(
        val name: String,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    // ------- DELETE CHAT -------
    data class ConfirmDeleteChat(
        val name: String,
        val countMessages: Int,
        val onConfirm: (deleteMessages: Boolean) -> Unit
    ) : ChatSessionUiEvent()

    data class ShowBlockSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowUnblockSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowChatHiddenSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowDeleteChatSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowToggleNotificationSuccess(
        val name: String,
        val enabled: Boolean
    ) : ChatSessionUiEvent()

    data class OtherUserNoLongerAvailable(
        val userName: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowBlockedByOther(
        val userName: String,
    ) : ChatSessionUiEvent()

    data class OpenPhotoSourcePicker(
        val onPickCamera: (() -> Unit)? = null,
        val onPickGallery: (() -> Unit)? = null
    ) : ChatSessionUiEvent()

    data class Error(
        val message: String
    ): ChatSessionUiEvent()




}


