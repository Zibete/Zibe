package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import java.io.Serializable

data class UserGroup(
    val userId: String = "",
    val userName: String = "",
    val type: Int = 0,
    val joinedAtMs: Long = 0L,
    val displayName: String = "",
    val alias: String = "",
    val aliasKey: String = "",
    val photoUrl: String = ""
) : Comparable<UserGroup>, Serializable {

    val isAnonymous: Boolean
        get() = type == ANONYMOUS_USER

    fun resolvedDisplayName(): String =
        if (isAnonymous) alias.ifBlank { userName }
        else displayName.ifBlank { userName }

    override fun compareTo(other: UserGroup): Int =
        other.resolvedDisplayName().compareTo(resolvedDisplayName(), ignoreCase = true)

    override fun equals(other: Any?): Boolean {
        return other is UserGroup &&
                userId == other.userId &&
                userName == other.userName
    }

    override fun hashCode(): Int = userId.hashCode() + userName.hashCode()
}
