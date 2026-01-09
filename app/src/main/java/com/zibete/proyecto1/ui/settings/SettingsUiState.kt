package com.zibete.proyecto1.ui.settings

data class SettingsUiState(
    val emailDisplay: String = "",
    val isLoading: Boolean = false,

    val providerLabel: String? = null, // "Google" / "Facebook" / null
    val canChangeCredentials: Boolean = true,

    val groupNotificationsEnabled: Boolean = true,
    val individualNotificationsEnabled: Boolean = true,

    val isEmailSectionExpanded: Boolean = false,
    val isPasswordSectionExpanded: Boolean = false,

    val requiresPasswordForSensitiveActions: Boolean = true
)
