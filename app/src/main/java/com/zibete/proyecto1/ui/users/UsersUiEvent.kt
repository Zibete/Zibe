package com.zibete.proyecto1.ui.users

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class UsersUiEvent {
    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType
    ) : UsersUiEvent()

    data class ShowFilterDialog(
        val applyAgeFilter: Boolean,
        val applyOnlineFilter: Boolean,
        val minAge: Int,
        val maxAge: Int
    ) : UsersUiEvent()

    data class NavigateToChat(
        val userId: String
    ) : UsersUiEvent()

    data class NavigateToProfile(
        val userIds: ArrayList<String>,
        val startIndex: Int
    ) : UsersUiEvent()

}
