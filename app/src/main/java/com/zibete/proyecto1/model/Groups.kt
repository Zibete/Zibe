package com.zibete.proyecto1.model

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.io.Serializable

@Keep
data class Groups(
    var name: String = "",
    var data: String = "",
    @get:PropertyName("id_creator") @set:PropertyName("id_creator")
    var idCreator: String = "",
    var category: Int = 0,
    var users: Int = 0,
    @get:PropertyName("dateCreate") @set:PropertyName("dateCreate")
    var creationDateTime: String? = null
) : Comparable<Groups>, Serializable {

    override fun compareTo(other: Groups): Int {
        val thisUsers = this.users ?: 0
        val otherUsers = other.users ?: 0
        return otherUsers.compareTo(thisUsers) // orden descendente como en Java
    }
}
