package com.zibete.proyecto1.model

import java.io.Serializable
import java.util.Date

data class Conversation(
    var lastContent: String = "",
    var lastMessageAt: Long = 0L,
    var userId: String = "",
    var otherId: String = "",
    var otherName: String = "",
    var otherPhotoUrl: String = "",
    var state: String = "", // ej: "", CHAT_STATE_SILENT, CHAT_STATE_BLOQ
    var unreadCount: Int = 0,
    var seen: Int = 0
) : Comparable<Conversation>, Serializable {

    override fun compareTo(other: Conversation): Int {
        return other.lastMessageAt.compareTo(this.lastMessageAt)
    }

    fun clone(): Conversation = this.copy()
}
