package com.zibete.proyecto1.ui.main.chrome

import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText

data class ToolbarMenuConfig(
    val showSettings: Boolean = false,
    val showUnblockUsers: Boolean = false,
    val showUnhideChats: Boolean = false,
    val showFavorites: Boolean = false,
    val showSearch: Boolean = false,
    val showExitGroup: Boolean = false
)

data class MainDestinationUiState(
    val currentScreen: CurrentScreen = CurrentScreen.OTHER,
    val showToolbar: Boolean = true,
    val showBottomNav: Boolean = true,
    val showDrawer: Boolean = true,
    val showBack: Boolean = false,
    val showUsersFragmentSettings: Boolean = false,
    val title: UiText? = UiText.Dynamic(""),
    val useGroupNameTitle: Boolean = false,
    val selectedBottomNavItemId: Int? = null,
    val selectedDrawerItemId: Int? = null,
    val menuConfig: ToolbarMenuConfig = ToolbarMenuConfig()
)

class MainDestinationUiMapper {

    fun map(destinationId: Int): MainDestinationUiState = when (destinationId) {
        R.id.nav_users -> MainDestinationUiState(
            currentScreen = CurrentScreen.USERS,
            showToolbar = true,
            showBottomNav = true,
            showDrawer = true,
            showBack = false,
            showUsersFragmentSettings = true,
            title = CurrentScreen.USERS.titleRes,
            selectedBottomNavItemId = R.id.navBottomUsers,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showUnblockUsers = true,
                showUnhideChats = true,
                showFavorites = true,
                showSearch = true
            )
        )

        R.id.nav_chat_list -> MainDestinationUiState(
            currentScreen = CurrentScreen.CHAT,
            showToolbar = true,
            showBottomNav = true,
            showDrawer = true,
            showBack = false,
            showUsersFragmentSettings = false,
            title = CurrentScreen.CHAT.titleRes,
            selectedBottomNavItemId = R.id.navBottomChat,
            selectedDrawerItemId = R.id.action_index,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showUnblockUsers = true,
                showFavorites = true,
                showSearch = true
            )
        )

        R.id.nav_group_select -> MainDestinationUiState(
            currentScreen = CurrentScreen.GROUPS,
            showToolbar = true,
            showBottomNav = true,
            showDrawer = true,
            showBack = false,
            showUsersFragmentSettings = false,
            title = CurrentScreen.GROUPS.titleRes,
            selectedBottomNavItemId = R.id.navBottomGroups,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showFavorites = true,
                showSearch = true,
                showUnhideChats = true
            )
        )

        R.id.nav_group_host -> MainDestinationUiState(
            currentScreen = CurrentScreen.GROUPS,
            showToolbar = true,
            showBottomNav = true,
            showDrawer = false,
            showBack = true,
            showUsersFragmentSettings = false,
            title = CurrentScreen.GROUPS.titleRes,
            useGroupNameTitle = true,
            selectedBottomNavItemId = R.id.navBottomGroups,
            menuConfig = ToolbarMenuConfig(
                showSettings = true,
                showSearch = true,
                showUnhideChats = true
            )
        )

        R.id.nav_favorites -> MainDestinationUiState(
            currentScreen = CurrentScreen.FAVORITES,
            showToolbar = true,
            showBottomNav = true,
            showDrawer = true,
            showBack = false,
            showUsersFragmentSettings = false,
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
            showToolbar = false,
            showBottomNav = false,
            showDrawer = false,
            showBack = false,
            showUsersFragmentSettings = false,
            title = CurrentScreen.EDIT_PROFILE.titleRes,
            selectedDrawerItemId = R.id.action_edit_profile,
            menuConfig = ToolbarMenuConfig(
                showSettings = true
            )
        )

        R.id.settingsFragment -> MainDestinationUiState(
            currentScreen = CurrentScreen.OTHER,
            showToolbar = false,
            showBottomNav = false,
            showDrawer = false,
            showBack = false,
            showUsersFragmentSettings = false,
            title = UiText.Dynamic(""),
            selectedDrawerItemId = R.id.action_settings,
            menuConfig = ToolbarMenuConfig()
        )

        else -> MainDestinationUiState(
            currentScreen = CurrentScreen.OTHER,
            showToolbar = true,
            showBottomNav = true,
            showDrawer = true,
            showBack = false,
            showUsersFragmentSettings = false,
            title = UiText.Dynamic(""),
            menuConfig = ToolbarMenuConfig()
        )
    }
}
