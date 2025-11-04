package com.zibete.proyecto1.model

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class Chats(
    @get:PropertyName("mensaje") @set:PropertyName("mensaje")
    var message: String = "",
    var date: String = "",
    @get:PropertyName("envia") @set:PropertyName("envia")
    var sender: String = "",
    var type: Int = 0,
    @get:PropertyName("visto") @set:PropertyName("visto")
    var seen: Int = 0
) : Serializable {

    override fun equals(other: Any?): Boolean =
        other is Chats &&
                message == other.message &&
                date == other.date &&
                sender == other.sender

    override fun hashCode(): Int =
        listOf(date, message, sender).hashCode()
}
