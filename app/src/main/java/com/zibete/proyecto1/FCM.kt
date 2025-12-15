package com.zibete.proyecto1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHAT_MESSAGE
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_ID = "mensaje"
private const val NOTIFICATION_ID = 0

@AndroidEntryPoint
class FCM : FirebaseMessagingService() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val data = remoteMessage.data
        if (data.isEmpty()) return

        val myUid = userRepository.myUid
        if (myUid.isBlank()) return

        val novistos = data["novistos"].orEmpty()
        val userName = data["user"] ?: return
        val msg = data["msg"].orEmpty()
        val otherUid = data["id_user"] ?: return
        val type = data["type"] ?: return

        val ref = if (type == NODE_CURRENT_CHAT) NODE_CHAT_MESSAGE else NODE_GROUP_CHAT

        // type == NODE_CURRENT_CHAT => chat 1-1
        if (type == NODE_CURRENT_CHAT) {

            // Si el user está en un grupo, NO tiene nada que ver con chat 1-1.
            // Acá respetamos tu switch de notificaciones individuales
            if (userPreferencesRepository.individualNotifications) {

                val newQuery: Query =
                    firebaseRefsContainer.refDatos
                        .child(myUid)
                        .child(type) // NODE_CURRENT_CHAT
                        .orderByChild("noVisto")
                        .startAt(1.0)

                newQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) return

                        val childrenCount = dataSnapshot.childrenCount
                        var countMsgUnread = 0

                        for (snapshot in dataSnapshot.children) {
                            val unRead = snapshot.child("noVisto").getValue(Int::class.java) ?: 0
                            countMsgUnread += unRead
                        }

                        if (childrenCount > 1) {
                            val title = "$countMsgUnread mensajes de $childrenCount chats"
                            val text = "$userName: $msg"
                            msgNotify(title, text)
                        } else {
                            val title = if (novistos == "1") {
                                "Nuevo mensaje de $userName"
                            } else {
                                "$novistos mensajes de $userName"
                            }
                            msgNotify(title, msg)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) = Unit
                })

            } else {
                // modo agrupado/silencioso => igual seteamos doble check
                doubleCheck(myUid, otherUid, type, ref)
            }

            return
        }

        // Si no es NODE_CURRENT_CHAT, en tu payload "type" te queda como nombre del grupo
        val groupName = type

        if (userPreferencesRepository.groupNotifications && data.isNotEmpty()) {

            val isInActiveGroup =
                userPreferencesRepository.inGroup &&
                        userPreferencesRepository.groupName.isNotEmpty() &&
                        userPreferencesRepository.groupName == groupName

            if (!isInActiveGroup) {
                val title = "Nuevo mensaje de $groupName"
                val text = "$userName: $msg"
                msgNotify(title, text, otherUid, groupName, ref)
            }
        }
    }

    private fun msgNotify(
        title: String?,
        text: String?,
        otherUid: String? = null,
        type: String? = null,
        ref: String? = null
    ) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = getString(R.string.channel_name)
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.setShowBadge(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setAutoCancel(true)
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

        // Doble check solo aplica a chat 1-1
        if (otherUid != null && type == NODE_CURRENT_CHAT && ref != null) {
            doubleCheck(userRepository.myUid, otherUid, type, ref)
        }
    }

    private fun doubleCheck(myUid: String, otherUid: String, type: String, ref: String) {

        firebaseRefsContainer.refDatos
            .child(myUid)
            .child(type)
            .child(otherUid)
            .child("wVisto")
            .setValue(2)

        firebaseRefsContainer.refDatos
            .child(myUid)
            .child(type)
            .child(otherUid)
            .child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val noVistos = dataSnapshot.getValue(Int::class.java) ?: 0
                    if (noVistos <= 0) return

                    // ✅ alineado a tu repo: chatId ordenado con "_"
                    val chatId = userRepository.getChatIdWith(otherUid)

                    firebaseRefsContainer.refChatMessageRoot
                        .child(ref)
                        .child(chatId)
                        .orderByChild("date")
                        .limitToLast(noVistos)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ds: DataSnapshot) {
                                setDoubleCheck(myUid, ds)
                            }

                            override fun onCancelled(error: DatabaseError) = Unit
                        })
                }

                override fun onCancelled(error: DatabaseError) = Unit
            })
    }

    private fun setDoubleCheck(myUid: String, dataSnapshot: DataSnapshot) {
        if (!dataSnapshot.exists()) return

        for (snapshot in dataSnapshot.children) {
            val envia = snapshot.child("envia").getValue(String::class.java)
            if (envia != null && envia != myUid && snapshot.hasChild("visto")) {
                snapshot.ref.child("visto").setValue(2)
            }
        }
    }

    private fun pendingIntent(): PendingIntent {
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
}
