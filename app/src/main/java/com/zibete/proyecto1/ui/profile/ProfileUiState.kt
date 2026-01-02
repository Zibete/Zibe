package com.zibete.proyecto1.ui.profile

import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.core.constants.Constants.NODE_DM

data class ProfileUiState(
    val profile: Users? = null,
    val isLoading: Boolean = false,
    val isGroupMatch: Boolean = false,
    val isFavorite: Boolean = false,
    val iBlockedUser: Boolean = false,
    val userBlockedMe: Boolean = false,
    val chatState: String = NODE_DM, // Estado normal
)


