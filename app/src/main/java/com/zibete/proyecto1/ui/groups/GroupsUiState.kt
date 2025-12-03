package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.model.Groups

data class GroupsUiState(
    val isLoading: Boolean = false,
    val groups: List<Groups> = emptyList(),
    val originalGroups: List<Groups> = emptyList(),
    val error: String? = null
)
