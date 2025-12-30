package com.zibete.proyecto1.ui.custompermission

sealed class PermissionUiEvent {
    data object PermissionGranted : PermissionUiEvent()
    data object PermissionDenied : PermissionUiEvent()
}