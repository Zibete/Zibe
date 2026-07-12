package com.zibete.proyecto1.ui.custompermission

sealed interface PermissionUiEvent {
    data class RequestPermission(val request: PermissionRequest) : PermissionUiEvent
    data object Completed : PermissionUiEvent
    data object LocationDenied : PermissionUiEvent
}
