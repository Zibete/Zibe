package com.zibete.proyecto1.ui.chat

data class ChatHeaderState(
    val nameUser: String = "Cargando...",
    val userState: String = "Desconectado",
    val userPhotoUrl: String = "",
    val isBlocked: Boolean = false,
    val isConnected: Boolean = false,
    val shouldCloseChat: Boolean = false
)