package com.zibete.proyecto1.ui.chatlist

import com.zibete.proyecto1.model.Conversation

data class ChatListUiState(
    val isLoading: Boolean = true,
    val chats: List<Conversation> = emptyList(),
    val filteredChats: List<Conversation> = emptyList(),
    val showOnboarding: Boolean = false,
    val searchQuery: String = ""
)
