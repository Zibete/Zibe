package com.zibete.proyecto1.model

import java.io.Serializable

sealed class ChatChildEvent {
    data class Added(val item: ChatMessageItem) : ChatChildEvent()
    data class Changed(val item: ChatMessageItem) : ChatChildEvent()
    data class Removed(val item: ChatMessageItem) : ChatChildEvent()
}
data class ChatMessageItem(
    val id: String,
    val message: ChatMessage
)
data class ChatMessage(
    var content: String = "",
    var date: String = "",
    var senderUid: String = "",
    var type: Int = 0,
    var seen: Int = 0
) : Serializable {

    override fun equals(other: Any?): Boolean =
        other is ChatMessage &&
                content == other.content &&
                date == other.date &&
                senderUid == other.senderUid

    override fun hashCode(): Int =
        listOf(date, content, senderUid).hashCode()
}
