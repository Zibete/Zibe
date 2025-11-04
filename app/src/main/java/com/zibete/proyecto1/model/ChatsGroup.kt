package com.zibete.proyecto1.model

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class ChatsGroup(
    @get:PropertyName("mensaje") @set:PropertyName("mensaje")
    var message: String = "",
    @get:PropertyName("date") @set:PropertyName("date")
    var dateTime: String = "",
    var name: String = "",
    var id: String = "",
    @get:PropertyName("type_msg") @set:PropertyName("type_msg")
    var typeMsg: Int = 0,
    @get:PropertyName("type_user") @set:PropertyName("type_user")
    var typeUser: Int = 0
) : Serializable {

    override fun equals(other: Any?): Boolean =
        other is ChatsGroup &&
                message == other.message &&
                dateTime == other.dateTime &&
                name == other.name

    override fun hashCode(): Int =
        listOf(dateTime, message, name).hashCode()
}
