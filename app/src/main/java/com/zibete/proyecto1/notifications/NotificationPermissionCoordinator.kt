package com.zibete.proyecto1.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

enum class NotificationPermissionDecision {
    NONE,
    REQUEST_PERMISSION,
    OPEN_SETTINGS
}

fun notificationPermissionDecision(
    status: NotificationPermissionSnapshot,
    shouldShowRationale: Boolean,
    isExplicitUserAction: Boolean
): NotificationPermissionDecision = when {
    !isExplicitUserAction -> NotificationPermissionDecision.NONE
    status.requiresRuntimePermission &&
        !status.isRuntimePermissionGranted &&
        (!status.wasRequested || shouldShowRationale) ->
        NotificationPermissionDecision.REQUEST_PERMISSION
    else -> NotificationPermissionDecision.OPEN_SETTINGS
}

fun notificationSettingsIntent(context: Context): Intent {
    val primary = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    if (primary.resolveActivity(context.packageManager) != null) return primary

    return Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
}
