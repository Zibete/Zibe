package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.domain.roomsv2.RoomV2Conversation
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2Membership
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2Report
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role
import com.zibete.proyecto1.domain.roomsv2.RoomV2Thread
import com.zibete.proyecto1.ui.components.ZibeSnackType

const val ROOM_V2_ID_ARG = "roomId"
const val ROOM_V2_CONVERSATION_ARG = "conversationId"

enum class RoomV2HostTab { CHAT, PEOPLE, PRIVATES, REPORTS }

data class RoomV2HostUiState(
    val roomId: String = "",
    val room: RoomV2DirectoryItem? = null,
    val membership: RoomV2Membership? = null,
    val participants: List<RoomV2Identity> = emptyList(),
    val messages: List<RoomV2Message> = emptyList(),
    val conversations: List<RoomV2Conversation> = emptyList(),
    val reports: List<RoomV2Report> = emptyList(),
    val selectedTab: RoomV2HostTab = RoomV2HostTab.CHAT,
    val selectedConversationId: String? = null,
    val draft: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val showLeaveConfirm: Boolean = false,
    val error: UiText? = null,
) {
    val currentThread: RoomV2Thread
        get() = RoomV2Thread(roomId = roomId, conversationId = selectedConversationId)

    val currentConversation: RoomV2Conversation?
        get() = conversations.firstOrNull { it.conversationId == selectedConversationId }

    val myIdentityId: String?
        get() = membership?.identity?.identityId

    val myRole: RoomV2Role?
        get() = membership?.identity?.role

    val canModerate: Boolean
        get() = myRole == RoomV2Role.OWNER || myRole == RoomV2Role.MODERATOR

    val isOwner: Boolean
        get() = myRole == RoomV2Role.OWNER
}

sealed interface RoomV2HostEvent {
    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType,
    ) : RoomV2HostEvent

    data object NavigateBack : RoomV2HostEvent
}
