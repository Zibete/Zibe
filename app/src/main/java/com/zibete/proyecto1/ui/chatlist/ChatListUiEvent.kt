package com.zibete.proyecto1.ui.chatlist
import com.zibete.proyecto1.ui.chat.ChatUiEvent
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class ChatListUiEvent {

    data class ShowSnackbar(
        val message: String,
        val type: ZibeSnackType
    ) : ChatListUiEvent()

    // ---------------- HIDE CHAT ----------------
    data class ConfirmHideChat(val name: String, val onConfirm: () -> Unit) : ChatListUiEvent()
    data object ShowChatHidden : ChatListUiEvent()

    // ---------------- BLOCK / UNBLOCK ----------------
    data class ConfirmBlock(val name: String, val onConfirm: () -> Unit) : ChatListUiEvent()
    data class ConfirmUnblock(val name: String, val onConfirm: () -> Unit) : ChatListUiEvent()

    data class ShowBlockSuccess(val name: String) : ChatListUiEvent()
    data class ShowUnblockSuccess(val name: String) : ChatListUiEvent()

    // ---------------- DELETE CHAT ----------------
    data class ConfirmDeleteChat(val name: String, val onConfirm: () -> Unit) : ChatListUiEvent()
    data class ShowDeleteChatSuccess(val name: String) : ChatListUiEvent()
}
