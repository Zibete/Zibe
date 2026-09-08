package com.zibete.proyecto1.ui.main.chrome

import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText

data class ToolbarMenuConfig(
    val showSettings: Boolean = false,
    val showUnblockUsers: Boolean = false,
    val showUnhideChats: Boolean = false,
    val showFavorites: Boolean = false,
    val showSearch: Boolean = false,
    val showDiscoverFilter: Boolean = false
)

data class MainDestinationUiState(
    val currentScreen: CurrentScreen = CurrentScreen.OTHER,
    val showToolbar: Boolean = true,
    val showBottomNav: Boolean = true,
    val showAccountAvatar: Boolean = false,
    val showBack: Boolean = false,
    val title: UiText? = null,
    val selectedBottomNavItemId: Int? = null,
    val menuConfig: ToolbarMenuConfig = ToolbarMenuConfig()
)

class MainDestinationUiMapper {

    fun map(destinationId: Int): MainDestinationUiState = when (destinationId) {
        R.id.nav_users -> MainDestinationUiState(
            currentScreen = CurrentScreen.USERS,
            showToolbar = true,
            showBottomNav = true,
            showAccountAvatar = true,
            showBack = false,
            title = CurrentScreen.USERS.titleRes,
            selectedBottomNavItemId = R.id.navBottomUsers,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showUnblockUsers = true,
                showUnhideChats = true,
                showFavorites = true,
                showSearch = true,
                showDiscoverFilter = true
            )
        )

        R.id.nav_chat_list -> MainDestinationUiState(
            currentScreen = CurrentScreen.CHAT,
            showToolbar = true,
            showBottomNav = true,
            showAccountAvatar = true,
            showBack = false,
            title = CurrentScreen.CHAT.titleRes,
            selectedBottomNavItemId = R.id.navBottomChat,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showUnblockUsers = true,
                showUnhideChats = true,
                showFavorites = true,
                showSearch = true
            )
        )

        R.id.nav_group_select -> MainDestinationUiState(
            currentScreen = CurrentScreen.GROUPS,
            showToolbar = true,
            showBottomNav = true,
            showAccountAvatar = true,
            showBack = false,
            title = CurrentScreen.GROUPS.titleRes,
            selectedBottomNavItemId = R.id.navBottomGroups,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showFavorites = true,
                showSearch = true,
                showUnhideChats = true
            )
        )

        R.id.nav_room_v2_host -> MainDestinationUiState(
            currentScreen = CurrentScreen.GROUPS,
            showToolbar = false,
            showBottomNav = false,
            showAccountAvatar = false,
            showBack = false,
            title = CurrentScreen.GROUPS.titleRes,
            selectedBottomNavItemId = R.id.navBottomGroups,
            menuConfig = ToolbarMenuConfig()
        )

        R.id.nav_favorites -> MainDestinationUiState(
            currentScreen = CurrentScreen.FAVORITES,
            showToolbar = true,
            showBottomNav = true,
            showAccountAvatar = true,
            showBack = false,
            title = CurrentScreen.FAVORITES.titleRes,
            selectedBottomNavItemId = R.id.navBottomFavorites,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showFavorites = true,
                showSearch = true
            )
        )

        R.id.editProfileFragment -> MainDestinationUiState(
            currentScreen = CurrentScreen.EDIT_PROFILE,
            showToolbar = true,
            showBottomNav = false,
            showAccountAvatar = false,
            showBack = true,
            title = CurrentScreen.EDIT_PROFILE.titleRes,
            menuConfig = ToolbarMenuConfig(
                showSettings = true
            )
        )

        R.id.settingsFragment -> MainDestinationUiState(
            currentScreen = CurrentScreen.OTHER,
            showToolbar = true,
            showBottomNav = false,
            showAccountAvatar = false,
            showBack = true,
            title = CurrentScreen.SETTINGS.titleRes,
            menuConfig = ToolbarMenuConfig()
        )

        else -> MainDestinationUiState(
            currentScreen = CurrentScreen.OTHER,
            showToolbar = true,
            showBottomNav = true,
            showAccountAvatar = false,
            showBack = false,
            menuConfig = ToolbarMenuConfig()
        )
    }
}
