package com.zibete.proyecto1

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.Constants.CHAT
import com.zibete.proyecto1.utils.Constants.CHATWITH
import com.zibete.proyecto1.utils.Constants.UNKNOWN
import com.zibete.proyecto1.utils.FirebaseRefs.refChats
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.user

class FCM : FirebaseMessagingService() {

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    @SuppressLint("WrongThread") // por el acceso a MainActivity.toolbar
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val currentUser = user ?: return

        val data = remoteMessage.data
        val novistos = data["novistos"]
        val userName = data["user"] ?: return
        val msg = data["msg"] ?: ""
        val idUser = data["id_user"] ?: return
        val type = data["type"] ?: return

        val ref: String = if (type == CHATWITH) CHAT else UNKNOWN

        if (type != UsuariosFragment.groupName) {
            if (UsuariosFragment.individualNotifications) {
                if (data.isNotEmpty()) {
                    val newQuery: Query =
                        refDatos.child(currentUser.uid).child(type)
                            .orderByChild("noVisto")
                            .startAt(1.0)

                    newQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                val childrenCount =
                                    dataSnapshot.childrenCount //Cantidad de chats con mensajes no vistos

                                var countMsgUnread = 0

                                for (snapshot in dataSnapshot.children) {
                                    val unRead =
                                        snapshot.child("noVisto")
                                            .getValue(Int::class.java) ?: 0
                                    countMsgUnread += unRead
                                }

                                if (childrenCount > 1) {
                                    val title = "$countMsgUnread mensajes de $childrenCount chats"
                                    val text = "$userName: $msg"

                                    msgNotify(title, text, idUser, type, ref)
                                } else {
                                    val title = if (novistos == "1") {
                                        "Nuevo mensaje de $userName"
                                    } else {
                                        "$novistos mensajes de $userName"
                                    }
                                    msgNotify(title, msg, idUser, type, ref)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            } else {
                DoubleCheck(idUser, type, ref)
            }
        } else {
            if (UsuariosFragment.groupNotifications) {
                if (data.isNotEmpty()) {
                    if (MainActivity.toolbar != null) {
                        if (MainActivity.toolbar!!.title != UsuariosFragment.groupName) {
                            val title = "Nuevo mensaje de $type"
                            val text = "$userName: $msg"
                            msgNotify(title, text, idUser, type, ref)
                        }
                    } else {
                        val title = "Nuevo mensaje de $type"
                        val text = "$userName: $msg"
                        msgNotify(title, text, idUser, type, ref)
                    }
                }
            }
        }
    }

    fun msgNotify(title: String?, text: String?, idUser: String, type: String, ref: String) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)

        val name: CharSequence = getString(R.string.channel_name)
        val notificationChannel =
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.setShowBadge(true)
        notificationManager.createNotificationChannel(notificationChannel)

        builder.setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setContentInfo(getString(R.string.app_name))
            .setContentIntent(pendingIntent())
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        if (type != UsuariosFragment.groupName) {
            DoubleCheck(idUser, type, ref)
        }
    }

    fun DoubleCheck(idUser: String, type: String, ref: String) {
        val currentUser = user ?: return

        refDatos.child(currentUser.uid).child(type).child(idUser).child("wVisto").setValue(2)

        refDatos.child(currentUser.uid).child(type).child(idUser).child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val noVistos = dataSnapshot.getValue(Int::class.java) ?: 0

                    if (noVistos > 0) {
                        refChats.child(ref).child("${currentUser.uid} <---> $idUser")
                            .child("Mensajes").limitToLast(noVistos)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    setDoubleCheck(dataSnapshot)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })

                        refChats.child(ref).child("$idUser <---> ${currentUser.uid}")
                            .child("Mensajes").limitToLast(noVistos)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    setDoubleCheck(dataSnapshot)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun setDoubleCheck(dataSnapshot: DataSnapshot) {
        val currentUser = user ?: return

        if (dataSnapshot.exists()) {
            for (snapshot in dataSnapshot.children) {
                if (snapshot.hasChild("envia")) {
                    val envia = snapshot.child("envia").getValue(String::class.java)
                    if (envia != currentUser.uid && snapshot.hasChild("visto")) {
                        snapshot.ref.child("visto").setValue(2)
                    }
                }
            }
        }
    }

    fun pendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "mensaje"
        private const val NOTIFICATION_ID = 0
    }
}
