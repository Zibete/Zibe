package com.zibete.proyecto1.ui.chat

sealed class ChatHeaderState {
    data object Loading : ChatHeaderState()
    data class Loaded(
        val name: String?,
        val status: String,
        val photoUrl: String?,
        val isBlocked: Boolean = false,
        val notificationsEnabled: Boolean = true,
        val shouldCloseChat: Boolean = false // “El otro te bloqueó → cerrá el chat automáticamente”.
    ) : ChatHeaderState()

}

