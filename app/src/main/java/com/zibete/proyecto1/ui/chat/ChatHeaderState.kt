package com.zibete.proyecto1.ui.chat

sealed class ChatHeaderState {
    data object Loading : ChatHeaderState()

    data class Loaded(
        val name: String,
        val status: String,
        val photoUrl: String,
        val isBlocked: Boolean = false,
        val isConnected: Boolean = false,
        val shouldCloseChat: Boolean = false
    ) : ChatHeaderState()
}