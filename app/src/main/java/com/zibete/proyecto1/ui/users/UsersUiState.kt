package com.zibete.proyecto1.ui.users

import com.zibete.proyecto1.core.ui.UiText

data class UsersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val users: List<UsersRowUiModel> = emptyList(),
    val searchQuery: String = "",
    val applyAgeFilter: Boolean = false,
    val applyOnlineFilter: Boolean = false,
    val minAge: Int = 18,
    val maxAge: Int = 99,
    val error: UiText? = null,
    val pendingFirstContact: UsersRowUiModel? = null,
    val chatCheckUserId: String? = null,
    val favoriteActionUserId: String? = null
) {
    val hasActiveFilters: Boolean
        get() = applyAgeFilter || applyOnlineFilter
}
