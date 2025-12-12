package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.model.ChatMessage

data class ChatState(
    val showPhotoPicker: Boolean = false,
    val photoReady: Boolean = false,
    val textReady: Boolean = false,
    val pendingFileUrl: String? = null,
    val selectedMessages: Set<ChatMessage> = emptySet(),
    val deleteModeEnabled: Boolean = false

)
