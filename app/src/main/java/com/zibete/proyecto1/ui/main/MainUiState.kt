package com.zibete.proyecto1.ui.main

import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText

data class MainUiState(
    val showToolbar: Boolean = true,
    val showSettingsLayout: Boolean = false,
    val showBottomNav: Boolean = true,
    val currentScreen: CurrentScreen = CurrentScreen.OTHER,

    val chatListBadgeCount: Int = 0,
    val groupBadgeCount: Int = 0,

    val unreadGroupChatCount: Int = 0,
    val unreadPrivateMessagesCount: Int = 0
)

enum class CurrentScreen(val titleRes: UiText) {
    CHAT(UiText.StringRes(R.string.menu_chat)),
    USERS(UiText.StringRes(R.string.menu_users)),
    GROUPS(UiText.StringRes(R.string.menu_groups)),
    EDIT_PROFILE(UiText.StringRes(R.string.menu_edit_profile)),
    FAVORITES(UiText.StringRes(R.string.menu_favorites)),
    OTHER(UiText.Dynamic(""))
}
