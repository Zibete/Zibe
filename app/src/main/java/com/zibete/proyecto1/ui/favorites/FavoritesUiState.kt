package com.zibete.proyecto1.ui.favorites

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val favorites: List<FavoriteUserUi> = emptyList(),
    val filteredFavorites: List<FavoriteUserUi> = emptyList(),
    val showOnboarding: Boolean = false,
    val searchQuery: String = ""
)
