package com.zibete.proyecto1.model

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class UserGroup(
    var userId: String = "",
    var userName: String = "",
    var type: Int = 0
) : Comparable<UserGroup>, Serializable {

    override fun compareTo(other: UserGroup): Int =
        other.userName.compareTo(userName, ignoreCase = true)

    override fun equals(other: Any?): Boolean {
        return other is UserGroup &&
                userId == other.userId &&
                userName == other.userName
    }

    override fun hashCode(): Int = userId.hashCode() + userName.hashCode()
}
