package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.model.ChatMessage

data class ChatState(
    val showPhotoPicker: Boolean = false,
    val photoReady: Boolean = false,
    val textReady: Boolean = false,
    val pendingFileUrl: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val selectedMessages: Set<ChatMessage> = emptySet()
)
