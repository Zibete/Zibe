package com.zibete.proyecto1.model

import java.io.Serializable
import java.util.Objects

data class Chats(
    var message: String? = null,
    var date: String? = null,
    var sender: String? = null,
    var type: Int = 0,
    var seen: Int = 0
) : Serializable {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val chats = other as Chats
        return date == chats.date &&
                sender == chats.sender &&
                message == chats.message
    }

    override fun hashCode(): Int {
        return Objects.hash(date, message, sender)
    }
}