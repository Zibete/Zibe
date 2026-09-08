package com.zibete.proyecto1.ui.main

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.data.HiddenChat
import com.zibete.proyecto1.data.profile.BlockedUser
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class MainUiEvent {

    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType
    ) : MainUiEvent()

    data class HandleChatSessionEvent(
        val event: ChatSessionUiEvent
    ) : MainUiEvent()

    data class ShowUnblockUsersDialog(
        val users: List<BlockedUser>
    ) : MainUiEvent()

    data class ShowUnhideChatsDialog(
        val chats: List<HiddenChat>
    ) : MainUiEvent()

    // ------------------- Navegación BottomNav ------------------
    data object ToUsers : MainUiEvent()
    data object ToChat : MainUiEvent()
    data object ToGroupsSelect : MainUiEvent()
    data object ToFavorites : MainUiEvent()

    // ------------------- Menu ---------------------
    data object ToEditProfile : MainUiEvent()
    data object NavigateToSettings : MainUiEvent()

    // ------------------- Navegación global ---------------------
    data object BackToChat : MainUiEvent()
    data object BackExitAppOrCloseSearch : MainUiEvent()
    data object ConfirmLogout : MainUiEvent()
}
