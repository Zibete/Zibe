package com.zibete.proyecto1.ui.settings

data class SettingsUiState(
    val emailDisplay: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val providerLabel: String? = null, // "Google" / "Facebook" / null

    val canChangeCredentials: Boolean = true,
    val groupNotificationsEnabled: Boolean = true,

    val individualNotificationsEnabled: Boolean = true,
//    val isEmailSectionExpanded: Boolean = false,
//
//    val isPasswordSectionExpanded: Boolean = false,
    val requiresPasswordForSensitiveActions: Boolean = true,

    val currentEmail: String? = null,
    val emailInput: String? = null,

    val passwordEmailInput: String? = null,

    val passwordInput: String? = null,
    val newPasswordInput: String? = null,

    val isSaveEmailEnabled: Boolean = false,
    val isSavePassEnabled: Boolean = false,
){
    val isBusy: Boolean get() = isLoading || isSaving
}
