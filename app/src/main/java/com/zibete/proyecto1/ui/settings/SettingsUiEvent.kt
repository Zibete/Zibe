package com.zibete.proyecto1.ui.settings

sealed interface SettingsUiEvent {
    data object RequestNotificationPermission : SettingsUiEvent
    data object OpenNotificationSettings : SettingsUiEvent
}
