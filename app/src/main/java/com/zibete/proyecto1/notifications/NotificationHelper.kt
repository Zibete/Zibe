package com.zibete.proyecto1.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_DM_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_DM_TYPE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_ROOM_KEY
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_ROOM
import com.zibete.proyecto1.core.constants.Constants.PayloadKeys
import com.zibete.proyecto1.data.UnreadSummary
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun showChatSummaryNotification(
        summary: UnreadSummary,
        lastSenderName: String,
        lastMessage: String,
        conversationId: String // chatId o groupName para id estable
    ) {
        val title = when {
            summary.totalChats > 1 ->
                "${summary.totalUnread} mensajes de ${summary.totalChats} chats"
            summary.totalUnread <= 1 ->
                "Nuevo mensaje de $lastSenderName"
            else ->
                "${summary.totalUnread} mensajes de $lastSenderName"
        }

        Log.d(TAG, "Preparing DM notification chatId=${safeId(conversationId)}")
        showMessageNotification(
            notificationId = conversationId.hashCode(),
            title = title,
            text = "$lastSenderName: $lastMessage",
            openIntent = buildOpenPendingDmIntent(conversationId)
        )
    }

    fun showRoomNotification(
        roomKey: String,
        messageId: String,
        roomName: String,
        lastSenderName: String,
        lastMessage: String
    ) {
        val deduplicationPreferences = context.getSharedPreferences(
            ROOM_NOTIFICATION_DEDUPLICATION_PREFERENCES,
            Context.MODE_PRIVATE
        )
        if (deduplicationPreferences.getString(roomKey, null) == messageId) {
            Log.i(TAG, "Skipping duplicate room notification roomKey=${safeId(roomKey)}")
            return
        }

        val posted = showMessageNotification(
            notificationId = roomKey.hashCode(),
            title = context.getString(R.string.rooms_notification_title, roomName),
            text = context.getString(
                R.string.rooms_notification_text,
                lastSenderName,
                lastMessage
            ),
            openIntent = buildOpenPendingRoomIntent(roomKey)
        )
        if (posted) deduplicationPreferences.edit().putString(roomKey, messageId).apply()
    }

    fun showMessageNotification(
        notificationId: Int,
        title: String,
        text: String,
        openIntent: Intent
    ): Boolean {
        if (!canPostNotifications(notificationId)) return false

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val builder = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_notifications_24dp)
            .setContentTitle(title)
            .setContentText(text)
            .setContentInfo(context.getString(R.string.app_name))
            .setContentIntent(pendingIntent(openIntent))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        nm.notify(notificationId, builder.build())
        Log.d(TAG, "NotificationManager.notify executed notificationId=$notificationId")
        return true
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelName: CharSequence = context.getString(R.string.channel_name)
        val channel = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply { setShowBadge(true) }
        nm.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel ready channelId=$MESSAGE_CHANNEL_ID")
    }

    private fun canPostNotifications(notificationId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            Log.w(TAG, "notification permission denied notificationId=$notificationId")
        }
        return isGranted
    }

    private fun buildOpenMainIntent(): Intent =
        Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    private fun buildOpenPendingDmIntent(chatId: String): Intent =
        buildOpenMainIntent().apply {
            putExtra(EXTRA_PENDING_DM_TYPE, NODE_DM)
            putExtra(EXTRA_PENDING_DM_CHAT_ID, chatId)
            putExtra(PayloadKeys.TYPE, NODE_DM)
            putExtra(PayloadKeys.CHAT_ID, chatId)
        }

    private fun buildOpenPendingRoomIntent(roomKey: String): Intent =
        buildOpenMainIntent().apply {
            putExtra(EXTRA_PENDING_DM_TYPE, NODE_ROOM)
            putExtra(EXTRA_PENDING_ROOM_KEY, roomKey)
            putExtra(PayloadKeys.TYPE, NODE_ROOM)
            putExtra(PayloadKeys.ROOM_KEY, roomKey)
        }

    private fun pendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            intent.hashCode(), // requestCode distinto por intent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "ZibeFCM"
        private const val ROOM_NOTIFICATION_DEDUPLICATION_PREFERENCES =
            "room_notification_deduplication"
        const val MESSAGE_CHANNEL_ID = "mensaje"

        private fun safeId(value: String): String = when {
            value.length <= 4 -> "***"
            else -> "${value.take(3)}...${value.takeLast(2)}"
        }
    }
}
