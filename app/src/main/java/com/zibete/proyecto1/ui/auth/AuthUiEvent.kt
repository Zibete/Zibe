package com.zibete.proyecto1.ui.auth

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class AuthUiEvent {
    data class ShowSnackbar(val message: String,val type: ZibeSnackType) : AuthUiEvent()
    object NavigateToSplash : AuthUiEvent()
    // Para limpiar flags en SharedPreferences (deleteUser / deleteFirebaseAccount)
    object ClearDeletePrefs : AuthUiEvent()
}
