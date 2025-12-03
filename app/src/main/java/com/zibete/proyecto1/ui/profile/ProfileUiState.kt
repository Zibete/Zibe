package com.zibete.proyecto1.ui.profile

import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_CHATWITH

data class ProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val age: Int? = null,
    val description: String? = null,
    val distance: String = "",
    val photoUrl: String? = null,
    val unknownName: String? = null,
    val hasGroupAlias: Boolean = false,
    val isFavorite: Boolean = false,
    val iBlockedUser: Boolean = false,
    val userBlockedMe: Boolean = false,
    val chatState: String = CHAT_STATE_CHATWITH,
    val error: String? = null
)


