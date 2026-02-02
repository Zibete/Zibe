package com.zibete.proyecto1.ui.signup

import com.zibete.proyecto1.core.ui.UiText

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val birthDate: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val birthDateError: UiText? = null,
    val nameError: UiText? = null,
    val age: Int? = null
)
