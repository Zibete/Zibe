package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface GroupsUiEvent {

    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType,
    ) : GroupsUiEvent

    data class NickInUse(val nick: String) : GroupsUiEvent
    data class GroupNameInUse(val name: String) : GroupsUiEvent
    object NavigateToGroupHost : GroupsUiEvent
}
