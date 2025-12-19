package com.zibete.proyecto1.model

import java.io.Serializable

data class ChatGroup(
    var content: String = "",
    var date: String = "",
    var nameUser: String = "", // <-- debería leer RTDB con senderUid?
    var senderUid: String = "",
    var type: Int = 0,
    var userType: Int = 0 // <-- debería leer RTDB con senderUid?

) : Serializable {

    override fun equals(other: Any?): Boolean =
        other is ChatGroup &&
                content == other.content &&
                date == other.date &&
                nameUser == other.nameUser

    override fun hashCode(): Int =
        listOf(date, content, nameUser).hashCode()
}
