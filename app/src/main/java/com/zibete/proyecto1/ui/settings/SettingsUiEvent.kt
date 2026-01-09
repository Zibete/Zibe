package com.zibete.proyecto1.ui.settings

import android.content.Intent
import com.zibete.proyecto1.ui.auth.AuthUiEvent

sealed interface SettingsUiEvent {
    data class ShowSnack(val message: String) : SettingsUiEvent
    data class ShowProgress(val message: String) : SettingsUiEvent
    data object HideProgress : SettingsUiEvent
    data class NavigateToSplash(val finish: Boolean = true) : SettingsUiEvent
}
