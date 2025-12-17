package com.zibete.proyecto1.ui.settings

import android.content.Intent

sealed interface SettingsUiEvent {
    data class ShowSnack(val message: String) : SettingsUiEvent
    data class ShowProgress(val message: String) : SettingsUiEvent
    data object HideProgress : SettingsUiEvent
    data class Navigate(val intent: Intent, val finish: Boolean = true) : SettingsUiEvent
}
