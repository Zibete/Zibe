package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.model.GroupChatItem
import com.zibete.proyecto1.model.UserGroup

enum class GroupHostTab { USERS, GROUP_CHAT, PRIVATE_CHATS }

data class GroupHostUiState(
    val isLoading: Boolean = false,

    val groupContext: GroupContext? = null,

    val selectedTab: GroupHostTab = GroupHostTab.GROUP_CHAT,

    val users: List<UserGroup> = emptyList(),
    val messages: List<GroupChatItem> = emptyList(),

    val totalMessages: Int = 0,
    val readMessages: Int = 0,
    val unreadMessages: Int = 0,

    val isSending: Boolean = false,

    val maxChatSize: Int = 10_000
)
