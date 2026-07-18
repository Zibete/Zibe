package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface GroupsUiEvent {
    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType
    ) : GroupsUiEvent

    data object NavigateToGroupHost : GroupsUiEvent
}
