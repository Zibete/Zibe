package com.zibete.proyecto1.notifications

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.zibete.proyecto1.R

enum class NotificationPermissionDecision {
    NONE,
    EXPLAIN_AND_REQUEST,
    OPEN_SETTINGS
}

fun notificationPermissionDecision(
    sdkInt: Int,
    isGranted: Boolean,
    wasRequested: Boolean,
    shouldShowRationale: Boolean
): NotificationPermissionDecision = when {
    sdkInt < Build.VERSION_CODES.TIRAMISU || isGranted -> NotificationPermissionDecision.NONE
    !wasRequested || shouldShowRationale -> NotificationPermissionDecision.EXPLAIN_AND_REQUEST
    else -> NotificationPermissionDecision.OPEN_SETTINGS
}

class NotificationPermissionCoordinator(
    private val activity: ComponentActivity
) {
    private val preferences = activity.getSharedPreferences(PREFERENCES, 0)
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { preferences.edit().putBoolean(KEY_REQUESTED, true).apply() }

    fun requestInContext() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val decision = notificationPermissionDecision(
            sdkInt = Build.VERSION.SDK_INT,
            isGranted = ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED,
            wasRequested = preferences.getBoolean(KEY_REQUESTED, false),
            shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
        )
        when (decision) {
            NotificationPermissionDecision.NONE -> Unit
            NotificationPermissionDecision.EXPLAIN_AND_REQUEST -> showExplanation(permission)
            NotificationPermissionDecision.OPEN_SETTINGS -> showSettingsExplanation()
        }
    }

    private fun showExplanation(permission: String) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.action_continue) { _, _ -> launcher.launch(permission) }
            .setNegativeButton(R.string.action_not_now, null)
            .show()
    }

    private fun showSettingsExplanation() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.notification_permission_settings_title)
            .setMessage(R.string.notification_permission_settings_message)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                activity.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${activity.packageName}")
                    )
                )
            }
            .setNegativeButton(R.string.action_not_now, null)
            .show()
    }

    private companion object {
        const val PREFERENCES = "notification_permission"
        const val KEY_REQUESTED = "requested"
    }
}
