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
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.constants.Constants.NODE_TYPE_CHATS
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_CHATWITH
import com.zibete.proyecto1.ui.constants.Constants.NODE_TYPE_UNKNOWN
import com.zibete.proyecto1.utils.FirebaseRefs.refChats
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FCM : FirebaseMessagingService() {

    @Inject
    lateinit var repo: UserPreferencesRepository

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    private val user get() = currentUser!!

    @SuppressLint("WrongThread") // por el acceso a MainActivity.toolbar
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val data = remoteMessage.data
        val novistos = data["novistos"]
        val userName = data["user"] ?: return
        val msg = data["msg"] ?: ""
        val idUser = data["id_user"] ?: return
        val type = data["type"] ?: return

        val ref: String = if (type == CHAT_STATE_CHATWITH) NODE_TYPE_CHATS else NODE_TYPE_UNKNOWN

        if (type != repo.groupName) {
            if (repo.individualNotifications) {
                if (data.isNotEmpty()) {
                    val newQuery: Query =
                        refDatos.child(user.uid).child(type)
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
                doubleCheck(idUser, type, ref)
            }
        } else {
            if (repo.groupNotifications && data.isNotEmpty()) {
                // Si el usuario está dentro del grupo activo y es el mismo grupo, no notificamos
                val isInActiveGroup = repo.inGroup &&
                        repo.groupName.isNotEmpty() &&
                        repo.groupName == type

                if (!isInActiveGroup) {
                    val title = "Nuevo mensaje de $type"
                    val text = "$userName: $msg"
                    msgNotify(title, text, idUser, type, ref)
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

        if (type != repo.groupName) {
            doubleCheck(idUser, type, ref)
        }
    }

    fun doubleCheck(idUser: String, type: String, ref: String) {

        refDatos.child(user.uid).child(type).child(idUser).child("wVisto").setValue(2)

        refDatos.child(user.uid).child(type).child(idUser).child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val noVistos = dataSnapshot.getValue(Int::class.java) ?: 0

                    if (noVistos > 0) {
                        refChats.child(ref).child("${user.uid} <---> $idUser")
                            .child("Mensajes").limitToLast(noVistos)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    setDoubleCheck(dataSnapshot)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })

                        refChats.child(ref).child("$idUser <---> ${user.uid}")
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

        if (dataSnapshot.exists()) {
            for (snapshot in dataSnapshot.children) {
                if (snapshot.hasChild("envia")) {
                    val envia = snapshot.child("envia").getValue(String::class.java)
                    if (envia != user.uid && snapshot.hasChild("visto")) {
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
