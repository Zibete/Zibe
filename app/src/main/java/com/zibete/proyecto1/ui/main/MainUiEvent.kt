package com.zibete.proyecto1.ui.main

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class MainUiEvent {

    data class ShowSnack(
        val uiText: UiText,
        val type: ZibeSnackType
    ) : MainUiEvent()

    // ------------------- Navegación BottomNav ------------------
    data object ToUsers : MainUiEvent()
    data object ToChat : MainUiEvent()
    data object ToGroupsSelect : MainUiEvent()         // No está en grupo → ir a nav_groups
    data object ToFavorites : MainUiEvent()
    data object ToGroupHost : MainUiEvent()

    // ------------------- Menu ---------------------
    data object ToEditProfile : MainUiEvent()
    data object NavigateToSettings : MainUiEvent()

    // ------------------- Desde chat o grupos para volver al listado ---------------------
    data object BackToChat : MainUiEvent()

    // ------------------- Salir de la app / logout / salir de grupo ---------------------
    data object ToGroupsAfterExit : MainUiEvent()

    data class NavigateToSplash(
        val sessionConflict: Boolean = false
    ) : MainUiEvent()

    data object BackExitAppOrCloseSearch : MainUiEvent()
    data object ConfirmExitGroup : MainUiEvent()
    data object ConfirmLogout : MainUiEvent()

}
