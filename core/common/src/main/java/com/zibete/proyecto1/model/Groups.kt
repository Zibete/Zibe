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
    var totalMessages: Int = 0

) : Comparable<Groups>, Serializable {

    override fun compareTo(other: Groups): Int {
        val thisUsers = this.users
        val otherUsers = other.users
        return otherUsers.compareTo(thisUsers) // orden descendente como en Java
    }
}
