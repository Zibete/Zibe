package com.zibete.proyecto1.ui.users

import com.zibete.proyecto1.model.Users

data class UsersUiState(
    val isLoading: Boolean = false,
    val users: List<Users> = emptyList(),
    val errorMessage: String? = null,
)