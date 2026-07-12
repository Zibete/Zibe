package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.model.ChatMessageItem

data class ChatState(
    val isActionLoading: Boolean = false,
    val showPhotoPicker: Boolean = false,
    val photoReady: Boolean = false,
    val textReady: Boolean = false,
    val pendingFileUrl: String? = null,
    val pendingPhotoUri: String? = null,
    val isRecording: Boolean = false,
    val messages: List<ChatMessageItem> = emptyList(),
    val selectedIds: Set<String> = emptySet()
)

data class ChatUiState(
    val header: ChatHeaderState = ChatHeaderState.Loading,
    val chat: ChatState = ChatState()
)
