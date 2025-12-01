package com.zibete.proyecto1.ui.chatlist

import com.zibete.proyecto1.model.ChatWith

data class ChatListUiState(
    val isLoading: Boolean = true,
    val chats: List<ChatWith> = emptyList(),
    val filteredChats: List<ChatWith> = emptyList(),
    val showOnboarding: Boolean = false,
    val searchQuery: String = ""
)
