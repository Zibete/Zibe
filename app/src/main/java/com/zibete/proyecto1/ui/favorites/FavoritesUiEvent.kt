package com.zibete.proyecto1.ui.favorites

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface FavoritesUiEvent {
    data class ShowSnack(
        val uiText: UiText,
        val snackType: ZibeSnackType,
    ) : FavoritesUiEvent
}
