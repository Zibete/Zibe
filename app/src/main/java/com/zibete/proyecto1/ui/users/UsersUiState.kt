package com.zibete.proyecto1.ui.users

data class UsersUiState(
    val isLoading: Boolean = false,
    val users: List<UsersRowUiModel> = emptyList()
)
