package com.zibete.proyecto1.model

import java.io.Serializable
sealed class GroupChatChildEvent {
    data class Added(val item: GroupChatItem) : GroupChatChildEvent()
    data class Changed(val item: GroupChatItem) : GroupChatChildEvent()
    data class Removed(val item: GroupChatItem) : GroupChatChildEvent()
}

data class GroupChatItem(
    val id: String,
    val message: ChatGroup
)

data class ChatGroup(
    var content: String = "",
    var timestamp: Long = 0,
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
