package com.zibete.proyecto1.ui.main

import android.content.Intent
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class MainNavEvent {
    data object ToSplashSessionConflict : MainNavEvent()
    // ------------------- Navegación BottomNav ------------------
    data object ToUsers : MainNavEvent()
    data object ToChat : MainNavEvent()
    data object ToGroupsSelect : MainNavEvent()         // No está en grupo → ir a nav_groups
    data object ToFavorites : MainNavEvent()
    data object ToGroupHost : MainNavEvent()
    // ------------------- Menu ---------------------
    data object ToSettings : MainNavEvent()
    data object ToEditProfile : MainNavEvent()
    data class BackFromEditProfile(
        val message: String ?= "",
    ) : MainNavEvent()

    // ------------------- Desde chat o grupos para volver al listado ---------------------
    data object BackToChat : MainNavEvent()

    // ------------------- Salir de la app / logout / salir de grupo ---------------------
    data object ToGroupsAfterExit : MainNavEvent()
    data class ToSplashAfterLogout(val intent: Intent) : MainNavEvent()
    data object BackExitAppOrCloseSearch : MainNavEvent()
    data object ConfirmExitGroup : MainNavEvent()

    data object ConfirmLogout : MainNavEvent()


    data class ShowMessage(
        val message: String,
        val type: ZibeSnackType? = ZibeSnackType.INFO
    ) : MainNavEvent()

}