package com.zibete.proyecto1.ui.main.chrome

import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText

enum class CurrentScreen(val titleRes: UiText) {
    CHAT(UiText.StringRes(R.string.menu_chat)),
    USERS(UiText.StringRes(R.string.menu_users)),
    GROUPS(UiText.StringRes(R.string.menu_groups)),
    EDIT_PROFILE(UiText.StringRes(R.string.menu_edit_profile)),
    SETTINGS(UiText.StringRes(R.string.menu_settings)),
    FAVORITES(UiText.StringRes(R.string.menu_favorites)),
    OTHER(UiText.Dynamic(""))
}
