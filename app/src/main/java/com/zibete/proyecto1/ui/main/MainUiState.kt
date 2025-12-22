package com.zibete.proyecto1.ui.main

data class MainUiState(
    val toolbarVisible: Boolean = true,
    val layoutSettingsVisible: Boolean = false,
    val bottomNavVisible: Boolean = true,
    val toolbarTitle: String = "",
    val currentScreen: CurrentScreen = CurrentScreen.OTHER,

    val chatListBadgeCount: Int = 0,
    val groupBadgeCount: Int = 0,

    val unreadGroupChatCount: Int = 0,
    val unreadPrivateMessagesCount: Int = 0,

    )
