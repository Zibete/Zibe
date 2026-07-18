package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.core.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.UserGroup

enum class GroupHostTab { USERS, GROUP_CHAT, PRIVATE_CHATS }

sealed interface RoomSendDraft {
    data class Text(val content: String) : RoomSendDraft
    data class Photo(val uri: String) : RoomSendDraft
}

data class GroupHostUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val groupContext: GroupContext? = null,
    val roomDescription: String = "",
    val creatorUid: String = "",
    val currentUid: String = "",
    val selectedTab: GroupHostTab = GroupHostTab.GROUP_CHAT,
    val isScreenVisible: Boolean = false,
    val users: List<UserGroup> = emptyList(),
    val messages: List<ChatGroupItem> = emptyList(),
    val privateConversations: List<Conversation> = emptyList(),
    val publicUnread: Int = 0,
    val composerText: String = "",
    val isSending: Boolean = false,
    val failedSend: RoomSendDraft? = null,
    val selectedMember: UserGroup? = null,
    val maxChatSize: Int = MAX_CHAT_SIZE
) {
    val roomKey: String
        get() = groupContext?.roomKey
            ?.ifBlank { groupContext.groupName }
            .orEmpty()

    val roomDisplayName: String
        get() = groupContext?.displayName
            ?.ifBlank { groupContext.groupName }
            .orEmpty()

    val privateUnread: Int
        get() = privateConversations.sumOf { it.unreadCount.coerceAtLeast(0) }

    val latestMessageAt: Long
        get() = messages.maxOfOrNull { it.message.timestamp } ?: 0L

    fun isCurrentUser(member: UserGroup): Boolean =
        member.userId.isNotBlank() && member.userId == currentUid

    fun isCreator(member: UserGroup): Boolean =
        creatorUid.isNotBlank() && member.userId == creatorUid
}

internal fun GroupHostUiState.reduce(event: GroupChatChildEvent): GroupHostUiState =
    when (event) {
        is GroupChatChildEvent.Added -> copy(
            messages = (messages.filterNot { it.id == event.item.id } + event.item)
                .sortedBy { it.message.timestamp }
                .takeLast(maxChatSize)
        )

        is GroupChatChildEvent.Changed -> copy(
            messages = messages.map { item ->
                if (item.id == event.item.id) event.item else item
            }
        )

        is GroupChatChildEvent.Removed -> copy(
            messages = messages.filterNot { it.id == event.id.id }
        )
    }

internal fun shouldExposeActiveRoom(state: GroupHostUiState): Boolean =
    state.isScreenVisible &&
        !state.isLoading &&
        !state.loadError &&
        state.selectedTab == GroupHostTab.GROUP_CHAT &&
        state.roomKey.isNotBlank()
