package com.zibete.proyecto1.ui.chat.session
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class ChatSessionUiEvent {

    // ------- HIDE CHAT (solo lo usarás en la lista) -------
    data class ConfirmHideChat(
        val name: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data object ShowChatHidden : ChatSessionUiEvent()

    // ------- BLOCK / UNBLOCK -------
    data class ConfirmBlock(
        val name: String,
        val onConfirm: suspend () -> Unit
    ) : ChatSessionUiEvent()

    data class ConfirmUnblock(
        val name: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowBlockSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowUnblockSuccess(val name: String) : ChatSessionUiEvent()

    // ------- MUTE / UNMUTE (silent / notif del menú del perfil) -------
    data class ConfirmMute(
        val name: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data class ConfirmUnmute(
        val name: String,
        val onConfirm: () -> Unit
    ) : ChatSessionUiEvent()

    data class ShowMuteSuccess(val name: String) : ChatSessionUiEvent()
    data class ShowUnmuteSuccess(val name: String) : ChatSessionUiEvent()

    // ------- DELETE CHAT -------
    data class ConfirmDeleteChat(
        val name: String,
        val countMessages: Int,
        val onConfirm: (deleteMessages: Boolean) -> Unit
    ) : ChatSessionUiEvent()

    data class ShowDeleteChatSuccess(val name: String) : ChatSessionUiEvent()
}


