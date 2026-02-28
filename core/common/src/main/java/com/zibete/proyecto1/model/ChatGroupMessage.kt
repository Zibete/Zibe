package com.zibete.proyecto1.model

import java.io.Serializable

sealed interface GroupChatChildEvent {
    data class Added(val item: ChatGroupItem) : GroupChatChildEvent
    data class Changed(val item: ChatGroupItem) : GroupChatChildEvent
    data class Removed(val id: ChatGroupItem) : GroupChatChildEvent
}

data class ChatGroupItem(
    val id: String,
    val message: ChatGroup
)

data class ChatGroup(
    var content: String = "",
    var timestamp: Long = 0L,
    var nameUser: String = "",
    var senderUid: String = "",
    var chatType: Int = 0,
    var userType: Int = 0

) : Serializable {

    override fun equals(other: Any?): Boolean =
        other is ChatGroup &&
                content == other.content &&
                timestamp == other.timestamp &&
                nameUser == other.nameUser

    override fun hashCode(): Int =
        listOf(timestamp, content, nameUser).hashCode()
}
