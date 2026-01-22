package com.zibete.proyecto1.ui.settings

sealed interface SettingsUiEvent {

    data object NavigateToSplash : SettingsUiEvent
}
