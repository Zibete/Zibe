package com.zibete.proyecto1.ui.settings

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface SettingsUiEvent {

    data class ShowSnack(
        val uiText: UiText,
        val type: ZibeSnackType
    ) : SettingsUiEvent

    data object NavigateToSplash: SettingsUiEvent
}
