package com.zibete.proyecto1.ui.profile

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.model.Users

sealed interface ProfileContent {
    data object Loading : ProfileContent
    data object NotFound : ProfileContent
    data class Error(val uiText: UiText) : ProfileContent
    data class Ready(val profile: Users) : ProfileContent
}

data class ProfileUiState(
    val content: ProfileContent = ProfileContent.Loading,
    val isActionLoading: Boolean = false,
    val profile: Users? = null,
    val distanceLabel: String = "",
    val isGroupMatch: Boolean = false,
    val isFavorite: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isNotificationsSilenced: Boolean = false,
    val hasBlockedMe: Boolean = false,
    val canDeleteChat: Boolean = false
) {
    val canOpenChat: Boolean
        get() =
            content is ProfileContent.Ready &&
                    !isActionLoading &&
                    profile?.id?.isNotBlank() == true
}


