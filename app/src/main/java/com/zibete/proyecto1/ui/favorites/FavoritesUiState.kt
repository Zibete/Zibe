package com.zibete.proyecto1.ui.favorites

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val favorites: List<FavoriteUserUi> = emptyList()
)

