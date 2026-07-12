package com.zibete.proyecto1.ui.main

data class MainUiState(
    val chatListBadgeCount: Int = 0,
    val groupBadgeCount: Int = 0,

    val unreadGroupChatCount: Int = 0,
    val unreadPrivateMessagesCount: Int = 0,
    val isGlobalLoading: Boolean = false,
    val isAccountSheetOpen: Boolean = false,
    val accountName: String = "",
    val accountEmail: String = "",
    val accountPhotoUrl: String = ""
)
