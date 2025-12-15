package com.zibete.proyecto1.model

import com.google.firebase.database.PropertyName
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHAT_STATE
import java.io.Serializable
import java.util.Date

data class ChatWith(
    @get:PropertyName("wMsg") @set:PropertyName("wMsg")
    var msg: String = "",

    @get:PropertyName("wDate") @set:PropertyName("wDate")
    var dateTime: String = "",

    var date: Date? = null,

    @get:PropertyName("wEnvia") @set:PropertyName("wEnvia")
    var senderId: String = "",

    @get:PropertyName("wUserID") @set:PropertyName("wUserID")
    var userId: String = "",

    @get:PropertyName("wUserName") @set:PropertyName("wUserName")
    var userName: String = "",

    @get:PropertyName("wUserPhoto") @set:PropertyName("wUserPhoto")
    var userPhoto: String = "",

    @get:PropertyName(NODE_CHAT_STATE) @set:PropertyName(NODE_CHAT_STATE)
    var state: String = "",

    @get:PropertyName("noVisto") @set:PropertyName("noVisto")
    var msgReceivedUnread: Int = 0,

    @get:PropertyName("wVisto") @set:PropertyName("wVisto")
    var seen: Int = 0
) : Comparable<ChatWith>, Serializable {

    override fun compareTo(other: ChatWith): Int =
        compareValuesBy(this, other, { it.date }) // orden por fecha

    fun clone(): ChatWith = this.copy()
}
