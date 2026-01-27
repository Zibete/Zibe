package com.zibete.proyecto1.ui.settings

import com.zibete.proyecto1.core.ui.UiText

enum class SettingsAction { LOGOUT, UPDATE_EMAIL, UPDATE_PASSWORD, DELETE_ACCOUNT, SEND_FEEDBACK }

data class SettingsUiState(

    val currentEmail: String? = null,
    val providerLabel: String? = null,
    val appVersion: String = "",

    val canChangeCredentials: Boolean = true,
    val requiresReauthForSensitiveActions: Boolean = true,
    val groupNotificationsEnabled: Boolean = true,
    val individualNotificationsEnabled: Boolean = true,

    val loadingAction: SettingsAction? = null,

    val generalSheetError: UiText? = null,
    val newEmailError: UiText? = null,
    val currentPasswordError: UiText? = null,
    val newPasswordError: UiText? = null,
    val feedbackError: UiText? = null
)
