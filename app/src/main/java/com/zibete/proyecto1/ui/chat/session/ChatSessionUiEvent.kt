package com.zibete.proyecto1.ui.chat.session

import com.zibete.proyecto1.core.ui.UiText

sealed class ChatSessionUiEvent {

    // ------- HIDE CHAT (solo lo usarás en la lista) -------
    data class ConfirmHideChat(
        val name: String,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    // ------- BLOCK / UNBLOCK -------
//    data class ConfirmBlock(
//        val name: String,
//        val onConfirm: suspend () -> Unit
//    ) : ChatSessionUiEvent()
//
//    data class ConfirmUnblock(
//        val name: String,
//        val onConfirm: suspend () -> Unit
//    ) : ChatSessionUiEvent()

    // ------- DELETE CHAT -------
    data class ConfirmDeleteChat(
        val name: String,
        val countMessages: Int,
        val onConfirm: (deleteMessages: Boolean) -> Unit
    ) : ChatSessionUiEvent()

//    data class ShowBlockSuccess(val name: String) : ChatSessionUiEvent()
//    data class ShowUnblockSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowChatHiddenSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowDeleteMessagesSuccess(val count: Int) : ChatSessionUiEvent()

    data class ShowToggleNotificationSuccess(
        val name: String,
        val isNotificationsSilenced: Boolean
    ) : ChatSessionUiEvent()
    
    data class ConfirmBlockAction(
        val name: String,
        val isBlockedByMe: Boolean,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowToggleFavoriteSuccess(
        val name: String,
        val newFavoriteState: Boolean
    ) : ChatSessionUiEvent()

    data class ShowToggleBlockSuccess(
        val name: String,
        val isBlocked: Boolean
    ) : ChatSessionUiEvent()

    data class OtherUserNoLongerAvailable(
        val userName: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowBlockedByOther(
        val userName: String,
    ) : ChatSessionUiEvent()

    data class ShowErrorDialog(
        val uiText: UiText
    ): ChatSessionUiEvent()

    data object CloseChat : ChatSessionUiEvent()
}


