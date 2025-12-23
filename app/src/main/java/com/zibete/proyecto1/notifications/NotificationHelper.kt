package com.zibete.proyecto1.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.ChatRepository.UnreadSummary
import com.zibete.proyecto1.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val channelId = "mensaje"

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

        showMessageNotification(
            notificationId = conversationId.hashCode(),
            title = title,
            text = "$lastSenderName: $lastMessage",
            openIntent = buildOpenMainIntent()
        )
    }

    fun showGroupNotification(
        groupName: String,
        unreadCount: Int,
        lastSenderName: String,
        lastMessage: String
    ) {
        val title = when {
            unreadCount <= 1 -> "Nuevo mensaje de $groupName"
            else -> "$unreadCount mensajes de $groupName"
        }

        showMessageNotification(
            notificationId = groupName.hashCode(),
            title = title,
            text = "$lastSenderName: $lastMessage",
            openIntent = buildOpenMainIntent(/* luego: extras para abrir grupo */)
        )
    }

    fun showMessageNotification(
        notificationId: Int,
        title: String,
        text: String,
        openIntent: Intent
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val builder = NotificationCompat.Builder(context, channelId)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setContentInfo(context.getString(R.string.app_name))
            .setContentIntent(pendingIntent(openIntent))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        nm.notify(notificationId, builder.build())
    }

    private fun ensureChannel(nm: NotificationManager) {
        val channelName: CharSequence = context.getString(R.string.channel_name)
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply { setShowBadge(true) }
        nm.createNotificationChannel(channel)
    }

    private fun buildOpenMainIntent(): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    private fun pendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            intent.hashCode(), // requestCode distinto por intent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
