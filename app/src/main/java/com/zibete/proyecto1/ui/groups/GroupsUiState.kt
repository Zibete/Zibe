package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.domain.rooms.CreateRoomCommand
import com.zibete.proyecto1.domain.rooms.JoinRoomCommand
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.model.RoomSession

data class GroupsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val rooms: List<Groups> = emptyList(),
    val visibleRooms: List<Groups> = emptyList(),
    val searchQuery: String = "",
    val error: UiText? = null,
    val activeSession: RoomSession? = null,
    val sheet: RoomsSheet? = null,
    val pendingSwitch: PendingRoomSwitch? = null,
    val identityType: RoomIdentityType = RoomIdentityType.PUBLIC,
    val publicIdentityName: String = "",
    val publicIdentityPhotoUrl: String = "",
    val alias: String = "",
    val roomName: String = "",
    val roomDescription: String = "",
    val roomNameError: UiText? = null,
    val roomDescriptionError: UiText? = null,
    val identityError: UiText? = null
)

sealed interface RoomsSheet {
    data object Create : RoomsSheet
    data class Join(val room: Groups) : RoomsSheet
}

sealed interface PendingRoomSwitch {
    val currentSession: RoomSession

    data class Create(
        override val currentSession: RoomSession,
        val command: CreateRoomCommand
    ) : PendingRoomSwitch

    data class Join(
        override val currentSession: RoomSession,
        val command: JoinRoomCommand
    ) : PendingRoomSwitch
}
