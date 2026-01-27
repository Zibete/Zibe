package com.zibete.proyecto1.ui.auth

import com.zibete.proyecto1.core.ui.UiText

data class AuthUiState(
    val deleteAccount: Boolean = false,
    val isLoadingLogin: Boolean = false,
    val isLoadingResetPassword: Boolean = false,
    val isLoadingDoNotDelete: Boolean = false,
    val isLoadingDeleteAccount: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val resetPasswordEmailError: UiText? = null
)
