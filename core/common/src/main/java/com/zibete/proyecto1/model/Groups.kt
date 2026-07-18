package com.zibete.proyecto1.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class Groups(
    var name: String = "",
    var description: String = "",
    var creatorUid: String = "",
    var type: Int = 0,
    var users: Int = 0,
    var createdAt: Long = 0L,
    var totalMessages: Int = 0,
    var roomId: String = "",
    var roomKey: String = "",
    var displayName: String = "",
    var unreadCount: Int = 0,
    var lastActivityAt: Long = 0L

) : Comparable<Groups>, Serializable {

    fun resolvedRoomKey(persistedKey: String = ""): String =
        roomKey.ifBlank { roomId.ifBlank { persistedKey.ifBlank { name } } }

    fun resolvedDisplayName(persistedKey: String = ""): String =
        displayName.ifBlank { name.ifBlank { resolvedRoomKey(persistedKey) } }

    fun withLegacyFallback(persistedKey: String): Groups = copy(
        roomKey = resolvedRoomKey(persistedKey),
        displayName = resolvedDisplayName(persistedKey)
    )

    override fun compareTo(other: Groups): Int {
        val thisUsers = this.users
        val otherUsers = other.users
        return otherUsers.compareTo(thisUsers) // orden descendente como en Java
    }
}
