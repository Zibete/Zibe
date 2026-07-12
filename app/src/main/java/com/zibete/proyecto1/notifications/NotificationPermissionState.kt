package com.zibete.proyecto1.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationPermissionSnapshot(
    val requiresRuntimePermission: Boolean,
    val isRuntimePermissionGranted: Boolean,
    val wasRequested: Boolean,
    val areAppNotificationsEnabled: Boolean,
    val isMessageChannelEnabled: Boolean
) {
    val shouldRequestDuringOnboarding: Boolean
        get() = requiresRuntimePermission && !isRuntimePermissionGranted && !wasRequested
}

interface NotificationPermissionStateProvider {
    fun snapshot(): NotificationPermissionSnapshot
    fun markRequested()
}

@Singleton
class AndroidNotificationPermissionStateProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : NotificationPermissionStateProvider {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun snapshot(): NotificationPermissionSnapshot {
        val requiresRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val isRuntimePermissionGranted = !requiresRuntimePermission ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        return NotificationPermissionSnapshot(
            requiresRuntimePermission = requiresRuntimePermission,
            isRuntimePermissionGranted = isRuntimePermissionGranted,
            wasRequested = preferences.getBoolean(KEY_REQUESTED, false),
            areAppNotificationsEnabled = NotificationManagerCompat.from(context)
                .areNotificationsEnabled(),
            isMessageChannelEnabled = isMessageChannelEnabled()
        )
    }

    override fun markRequested() {
        preferences.edit().putBoolean(KEY_REQUESTED, true).apply()
    }

    private fun isMessageChannelEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(NotificationHelper.MESSAGE_CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    companion object {
        const val PREFERENCES = "notification_permission"
        const val KEY_REQUESTED = "requested"
    }
}
