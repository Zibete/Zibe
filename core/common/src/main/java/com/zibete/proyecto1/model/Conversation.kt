package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import java.io.Serializable

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

    fun isVisible(): Boolean {
        if (state == Constants.CHAT_STATE_BLOCKED) return false
        if (state == Constants.CHAT_STATE_HIDE) return false
        if (lastMessageAt == 0L && lastContent.isBlank()) return false
        return state == NODE_DM || state == CHAT_STATE_SILENT
    }
}
