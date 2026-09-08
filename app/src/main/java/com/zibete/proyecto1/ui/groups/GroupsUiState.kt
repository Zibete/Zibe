package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem

data class GroupsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val groups: List<RoomV2DirectoryItem> = emptyList(),
    val filteredGroups: List<RoomV2DirectoryItem> = emptyList(),
    val searchQuery: String = ""
)
