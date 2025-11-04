package com.zibete.proyecto1.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class Groups(
    var name: String? = null,
    var data: String? = null,
    var ID_creator: String? = null,
    var category: Int? = null,
    var users: Int? = null,
    var dateCreate: String? = null
) : Comparable<Groups>, Serializable {

    override fun compareTo(other: Groups): Int {
        val thisUsers = this.users ?: 0
        val otherUsers = other.users ?: 0
        return otherUsers.compareTo(thisUsers) // orden descendente como en Java
    }
}
