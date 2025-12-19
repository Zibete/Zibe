package com.zibete.proyecto1

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesDSRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.notifications.NotificationHelper
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ZibeFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var userPreferencesDSRepository: UserPreferencesDSRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var groupRepository: GroupRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) return

        val myUid = userRepository.myUid
        if (myUid.isBlank()) return

        serviceScope.launch {
            try {
                handleDataMessage(data, myUid)
            } catch (t: Throwable) {
                Log.e("ZibeFCM", "Error handling FCM", t)
            }
        }
    }

    private suspend fun handleDataMessage(
        data: Map<String, String>,
        myUid: String
    ) {
        val novistos = data["novistos"].orEmpty() // puede venir vacío
        val userName = data["user"] ?: return
        val msg = data["msg"].orEmpty()
        val otherUid = data["id_user"] ?: return
        val type = data["type"] ?: return

        // =========================
        // 1) CHAT 1-1 (NODE_CURRENT_CHAT)
        // =========================
        if (type == NODE_DM) {

            val enabled = userPreferencesDSRepository.individualNotificationsFlow.first()
            if (!enabled) {
                // Si el usuario desactivó notificaciones individuales:
                // igual aplicamos doble-check si corresponde, pero NO notificamos.
                chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, type)
                return
            }

            // Resumen real (fuente de verdad: Firebase)
            val summary = chatRepository.getUnreadSummaryForChats(myUid, type)

            // Si tu chatId es uid1_uid2 ordenado:
            val chatId = userRepository.getChatIdWith(otherUid)

            notificationHelper.showChatSummaryNotification(
                summary = summary,
                lastSenderName = userName,
                lastMessage = msg,
                conversationId = chatId
            )

            chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, type)
            return

//            val totalChats = summary.totalChats.coerceAtLeast(0)
//            val totalUnread = summary.totalUnread.coerceAtLeast(0)
//
//            // --- Títulos posibles (equivalentes a tu lógica legacy) ---
//            // A) Varios chats con mensajes sin leer => "X mensajes de Y chats"
//            if (totalChats > 1 && totalUnread > 0) {
//                val title = "$totalUnread mensajes de $totalChats chats"
//                val text = "$userName: $msg"
//                notificationHelper.showMessageNotification(title, text)
//
//                chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, type)
//                return
//            }
//
//            // B) Un solo chat
//            // - Si novistos viene => usamos ese valor como “mensajes de X”
//            // - Si no viene => caemos al totalUnread del repo
//            val unreadForThisChat = novistos.toIntOrNull()?.coerceAtLeast(0)
//                ?: totalUnread.coerceAtLeast(0)
//
//            // B1) Primer mensaje / 1 sin leer => "Nuevo mensaje de X"
//            // B2) Varios => "N mensajes de X"
//            val title = when {
//                unreadForThisChat <= 1 -> "Nuevo mensaje de $userName"
//                else -> "$unreadForThisChat mensajes de $userName"
//
//
//            //  Texto:
//            //  - Legacy: si era 1 chat -> msg puro
//            //  - Pero es más consistente: "user: msg" para que se entienda en lockscreen.
//            //  Si querés mantener exacto legacy, cambiá `text` por `msg`.
//            val text = "$userName: $msg"
//            notificationHelper.showMessageNotification(title, text)
//
//            chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, type)
//            return
        }

        // =========================
        // 2) GRUPO (type = groupName en tu payload)
        // =========================
        val groupName = type

        val groupEnabled = userPreferencesDSRepository.groupNotificationsFlow.first()
        if (!groupEnabled) return

        // Si el user está actualmente dentro de ese mismo grupo, NO notificamos (como antes)
        val ctx = userPreferencesDSRepository.groupContextFlow.first()
        val isInActiveGroup = (ctx?.inGroup == true && ctx.groupName == groupName)

        if (isInActiveGroup) return

        val unread = groupRepository.groupTabUnreadCountOnce(groupName)
        notificationHelper.showGroupNotification(
            groupName = groupName,
            unreadCount = unread,
            lastSenderName = userName,
            lastMessage = msg
        )

//        // Conteo de no leídos del grupo (para títulos “(N)”)
//        // Si tu repo todavía no lo tiene, crealo:
//        // fun groupUnreadCount(groupName: String): Int
//        val groupUnreadCount = try {
//            groupRepository.groupTabUnreadCountOnce(groupName) // <--- CREAR en GroupRepository (single read)
//        } catch (_: Throwable) {
//            0
//        }
//
//        // --- Títulos posibles (grupo) ---
//        // A) Si sabemos cantidad => "N mensajes de <grupo>"
//        // B) Si no sabemos => "Nuevo mensaje de <grupo>"
//        val title = when {
//            groupUnreadCount > 1 -> "$groupUnreadCount mensajes de $groupName"
//            groupUnreadCount == 1 -> "Nuevo mensaje de $groupName"
//            else -> "Nuevo mensaje de $groupName"
//        }
//
//        val text = "$userName: $msg"
//        notificationHelper.showMessageNotification(title, text)
    }
}
