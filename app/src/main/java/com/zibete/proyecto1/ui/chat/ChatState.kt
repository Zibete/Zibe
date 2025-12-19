package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem

data class ChatState(
    val showPhotoPicker: Boolean = false,
    val photoReady: Boolean = false,
    val textReady: Boolean = false,
    val pendingFileUrl: String? = null,
    val messages: List<ChatMessageItem> = emptyList(),
    val selectedIds: Set<String> = emptySet()
)