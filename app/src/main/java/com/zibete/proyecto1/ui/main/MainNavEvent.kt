package com.zibete.proyecto1.ui.main

import android.content.Intent

sealed class MainNavEvent {
    data object ToChat : MainNavEvent()
    data object ToUsers : MainNavEvent()
    data object ToFavorites : MainNavEvent()
    data object ToSplash : MainNavEvent()
    data object ToGroupsSelect : MainNavEvent()         // No está en grupo → ir a nav_groups
    data class ToGroupsDetail(val groupName: String, val userName: String) : MainNavEvent()
    data object ToEditProfile : MainNavEvent()
    data object ToGroupsAfterExit : MainNavEvent()
    data class ToSplashAfterLogout(val intent: Intent) : MainNavEvent()
    data object BackFromEditProfile : MainNavEvent()
    data object BackExitAppOrCloseSearch : MainNavEvent()
    data object BackToChat : MainNavEvent()
    data object ToSettings : MainNavEvent()
    data class ConfirmExitGroup(val groupName: String) : MainNavEvent()

}