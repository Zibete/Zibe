package com.zibete.proyecto1.ui.chat.session

import com.zibete.proyecto1.core.ui.UiText

sealed class ChatSessionUiEvent {

    // ------- HIDE -------
    data class ConfirmHideChat(
        val name: String,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowChatHiddenSuccess(
        val name: String
    ) : ChatSessionUiEvent()

    // ------- DELETE -------
    data class DeleteClickedChoiceMode(
        val name: String,
        val countMessages: Int,
        val onConfirm: (shouldDeleteMessages: Boolean) -> Unit
    ) : ChatSessionUiEvent()

    data class ConfirmDeleteChat(
        val name: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowDeleteMessagesSuccess(
        val count: Int
    ) : ChatSessionUiEvent()

    // ------- SILENT --------

    data class ShowToggleNotificationSuccess(
        val name: String,
        val isNotificationsSilenced: Boolean
    ) : ChatSessionUiEvent()

    // ------- BLOCK -------
    data class ConfirmToggleBlockAction(
        val name: String,
        val isBlockedByMe: Boolean,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowToggleBlockSuccess(
        val name: String,
        val isBlocked: Boolean
    ) : ChatSessionUiEvent()

    data class ShowBlockedByOther(
        val userName: String,
    ) : ChatSessionUiEvent()

    // ------- FAVORITE -------

    data class ShowToggleFavoriteSuccess(
        val name: String,
        val newFavoriteState: Boolean
    ) : ChatSessionUiEvent()

    // ------- USER -------

    data class OtherUserNoLongerAvailable(
        val userName: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    // ------- ERROR -------

    data class ShowErrorDialog(
        val uiText: UiText
    ) : ChatSessionUiEvent()

    data object CloseChat
        : ChatSessionUiEvent()
}


