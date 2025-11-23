package com.zibete.proyecto1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.zibete.proyecto1.model.Users

class BroadcastReciver : BroadcastReceiver() {


    private val usersList: MutableList<Users?>? = null // reservado si se requiere en el futuro

    override fun onReceive(context: Context, intent: Intent) {
        // 🚀 Arranque seguro del servicio Notify
        val serviceIntent = Intent(context, Notify::class.java)
        context.applicationContext.startForegroundService(serviceIntent)

        // ✅ Notificación opcional si se pasan datos en el intent
        if (intent.extras != null) {
            val noVistos = intent.getIntExtra("noVistos", 0)
            val ultMsg = intent.getStringExtra("ult_msg")
            val nombre = intent.getStringExtra("getNombre")

            if (noVistos > 0 && nombre != null && ultMsg != null) {
                showNotification(context, nombre, ultMsg, noVistos)
            }
        }
    }

    private fun showNotification(
        context: Context,
        nombre: String?,
        ultMsg: String?,
        noVistos: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        // 🔔 Crear canal de notificación si es necesario
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Notificaciones de mensajes",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Canal para mensajes entrantes"
        notificationManager?.createNotificationChannel(channel)

        // 📨 Construcción de la notificación
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_chat_24)
            .setContentTitle("$noVistos mensajes de $nombre")
            .setContentText(ultMsg)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "notify_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
