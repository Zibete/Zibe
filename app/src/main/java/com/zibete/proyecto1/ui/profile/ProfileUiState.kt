package com.zibete.proyecto1.ui.profile

import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT

data class ProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val age: Int? = null,
    val description: String? = null,
    val distance: String = "",
    val photoUrl: String? = null,
    val isGroupMatch: Boolean = false,
    val isFavorite: Boolean = false,
    val iBlockedUser: Boolean = false,
    val userBlockedMe: Boolean = false,
    val chatState: String = NODE_CURRENT_CHAT,
    val error: String? = null
)


