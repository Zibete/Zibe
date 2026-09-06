package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Membership
import com.zibete.proyecto1.ui.components.ZibeSnackType

data class RoomV2ListItem(
    val room: RoomV2DirectoryItem,
    val membership: RoomV2Membership? = null,
) {
    val isMember: Boolean get() = membership?.identity?.active == true
    val unreadCount: Long get() = membership?.unreadCount?.coerceAtLeast(0) ?: 0
}

data class RoomsV2UiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val rooms: List<RoomV2ListItem> = emptyList(),
    val visibleRooms: List<RoomV2ListItem> = emptyList(),
    val searchQuery: String = "",
    val error: UiText? = null,
    val sheet: RoomsV2Sheet? = null,
    val identityMode: RoomV2IdentityMode = RoomV2IdentityMode.REAL,
    val publicIdentityName: String = "",
    val alias: String = "",
    val roomName: String = "",
    val roomDescription: String = "",
    val roomNameError: UiText? = null,
    val roomDescriptionError: UiText? = null,
    val identityError: UiText? = null,
)

sealed interface RoomsV2Sheet {
    data object Create : RoomsV2Sheet
    data class Join(val room: RoomV2DirectoryItem) : RoomsV2Sheet
}

sealed interface RoomsV2UiEvent {
    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType,
    ) : RoomsV2UiEvent

    data class NavigateToRoom(val roomId: String) : RoomsV2UiEvent
}
