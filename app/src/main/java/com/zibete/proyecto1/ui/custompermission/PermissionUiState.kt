package com.zibete.proyecto1.ui.custompermission

enum class PermissionEducationMode {
    COMBINED,
    NOTIFICATION_ONLY
}

enum class PermissionRequest {
    LOCATION,
    NOTIFICATIONS
}

data class PermissionUiState(
    val mode: PermissionEducationMode = PermissionEducationMode.COMBINED,
    val showLocationRationaleDialog: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val requestInFlight: PermissionRequest? = null
)
