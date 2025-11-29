package com.zibete.proyecto1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.zibete.proyecto1.model.State
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Notify : Service() {

    companion object {
        private var CHANNEL_ID: String = "notify_channel"
        private const val NOTIFICATION_ID = 0
        @JvmStatic
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    }

    private lateinit var context: Context
    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var notification: Notification? = null
    private var notificationAña: Notification? = null
    private val refEstado: DatabaseReference? =
        user?.let {
            refDatos.child(it.uid).child("Estado")
        }

    override fun onCreate() {
        super.onCreate()
        context = this
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentUser = user
        if (currentUser != null) {
            CHANNEL_ID = currentUser.uid
        }

        val estadoRef = refEstado
        if (estadoRef != null) {
            val cState = State(
                getString(R.string.online),
                "",
                ""
            )
            estadoRef.setValue(cState)
        }

        val extras = intent?.extras
        if (extras != null) {
            val noVistos = extras.getInt("noVistos", 0)
            val ultMsg = extras.getString("ult_msg") ?: ""
            val nombre = extras.getString("getNombre") ?: ""
            val id = extras.getString("getID") ?: ""
            val foto = extras.getString("getFoto") ?: ""

            val snoozeIntent = Intent(this, BroadcastReciver::class.java).apply {
                putExtra("getNombre", nombre)
                putExtra("getFoto", foto)
                putExtra("getID", id)
                putExtra("noVistos", noVistos)
                putExtra("ult_msg", ultMsg)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                action = "com.zibete.action.SERVICE"
            }
            sendBroadcast(snoozeIntent)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_chat_24)
                .setContentTitle("$noVistos mensajes de $nombre")
                .setContentText(ultMsg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

            notificationAña = NotificationCompat.Builder(this, CHANNEL_ID)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification!!)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val estadoRef = refEstado
        if (estadoRef != null) {
            val c = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val cState = State(
                getString(R.string.ultVez),
                dateFormat.format(c.time),
                timeFormat.format(c.time)
            )
            estadoRef.setValue(cState)
        }
        super.onDestroy()
    }
}
