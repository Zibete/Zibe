package com.zibete.proyecto1.domain.rooms

import com.zibete.proyecto1.model.RoomIdentity
import com.zibete.proyecto1.model.RoomSession

data class CreateRoomCommand(
    val roomName: String,
    val description: String,
    val identity: RoomIdentity,
    val eventContent: String,
    val replaceActiveRoom: Boolean = false,
    val previousSession: RoomSession? = null,
    val leaveEventContent: String = ""
)

data class JoinRoomCommand(
    val roomKey: String,
    val displayName: String,
    val identity: RoomIdentity,
    val eventContent: String
)

data class SwitchRoomCommand(
    val previousSession: RoomSession,
    val target: JoinRoomCommand,
    val leaveEventContent: String
)

data class LeaveRoomCommand(
    val roomKey: String,
    val userName: String,
    val userType: Int,
    val eventContent: String
)

data class MarkRoomReadCommand(
    val roomKey: String,
    val lastReadAt: Long,
    val isChatVisible: Boolean
)

sealed interface RoomOperationResult {
    data class Created(val session: RoomSession) : RoomOperationResult
    data class Joined(val session: RoomSession) : RoomOperationResult
    data class Switched(
        val previousRoomKey: String,
        val session: RoomSession
    ) : RoomOperationResult

    data class Left(val roomKey: String) : RoomOperationResult
    data class NameInUse(val roomName: String) : RoomOperationResult
    data class AliasInUse(val alias: String) : RoomOperationResult
    data class AlreadyActive(val session: RoomSession) : RoomOperationResult
    data class SwitchRequired(
        val currentSession: RoomSession,
        val requestedRoomKey: String
    ) : RoomOperationResult

    data class ValidationFailed(val issues: List<RoomValidationIssue>) : RoomOperationResult
}

sealed interface MarkRoomReadResult {
    data class Marked(
        val roomKey: String,
        val lastReadAt: Long
    ) : MarkRoomReadResult

    data object SkippedNotVisible : MarkRoomReadResult
    data class ValidationFailed(val issues: List<RoomValidationIssue>) : MarkRoomReadResult
}
