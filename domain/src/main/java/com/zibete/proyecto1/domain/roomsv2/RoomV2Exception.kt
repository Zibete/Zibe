package com.zibete.proyecto1.domain.roomsv2

enum class RoomV2ErrorCode {
    UNAUTHENTICATED, PERMISSION_DENIED, ALIAS_TAKEN, ROOM_NAME_TAKEN, ROOM_CLOSED,
    IDENTITY_CHANGE_REQUIRED, OWNER_ACTION_REQUIRED, NOT_FOUND, INVALID_INPUT,
    OFFLINE, CONFLICT, INTERNAL,
}

class RoomV2Exception(
    val code: RoomV2ErrorCode,
    cause: Throwable? = null,
) : Exception(code.name, cause)
