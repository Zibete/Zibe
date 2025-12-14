package com.zibete.proyecto1.ui.favorites

sealed interface FavoritesUiEvent {
    data class ShowMessage(val message: String) : FavoritesUiEvent
    object ShowEmptyFavorites : FavoritesUiEvent
}
