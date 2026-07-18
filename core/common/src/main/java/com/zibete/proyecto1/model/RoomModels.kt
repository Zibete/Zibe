package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER

enum class RoomIdentityType(val legacyValue: Int) {
    ANONYMOUS(ANONYMOUS_USER),
    PUBLIC(PUBLIC_USER);

    companion object {
        fun fromLegacy(value: Int): RoomIdentityType =
            entries.firstOrNull { it.legacyValue == value } ?: PUBLIC
    }
}

data class RoomIdentity(
    val displayName: String,
    val type: RoomIdentityType,
    val photoUrl: String = ""
)

data class RoomSession(
    val roomKey: String,
    val displayName: String,
    val userName: String,
    val userType: Int
) {
    val identity: RoomIdentity
        get() = RoomIdentity(
            displayName = userName,
            type = RoomIdentityType.fromLegacy(userType)
        )
}
