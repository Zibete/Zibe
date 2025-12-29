package com.zibete.proyecto1

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.notifications.NotificationHelper
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.Constants.PayloadKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ZibeFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var userPreferencesProvider: UserPreferencesProvider
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
        val unreadCount = data[PayloadKeys.UNREAD_COUNT].orEmpty()
        val otherName = data[PayloadKeys.OTHER_NAME] ?: return
        val content = data[PayloadKeys.CONTENT].orEmpty()
        val otherId = data[PayloadKeys.OTHER_ID] ?: return
        val nodeType = data[PayloadKeys.TYPE] ?: return

        // =========================
        // 1) CHAT 1-1 (NODE_CURRENT_CHAT)
        // =========================
        if (nodeType == NODE_DM) {

            val enabled = userPreferencesProvider.individualNotificationsFlow.first()
            if (!enabled) {
                // Si el usuario desactivó notificaciones individuales:
                // igual aplicamos doble-check si corresponde, pero NO notificamos.
                chatRepository.applyDoubleCheckForLatestUnread(myUid, otherId, nodeType)
                return
            }

            // Resumen real (fuente de verdad: Firebase)

            val summary = chatRepository.getUnreadSummaryForChats(myUid, nodeType)

            // Si tu chatId es uid1_uid2 ordenado:
            val chatId = chatRepository.getChatId(otherId)

            notificationHelper.showChatSummaryNotification(
                summary = summary,
                lastSenderName = otherName,
                lastMessage = content,
                conversationId = chatId
            )

            chatRepository.applyDoubleCheckForLatestUnread(myUid, otherId, nodeType)
            return

        }

        // =========================
        // 2) GRUPO (type = groupName en tu payload)
        // =========================
        val groupName = nodeType

        val groupEnabled = userPreferencesProvider.groupNotificationsFlow.first()
        if (!groupEnabled) return

        // Si el user está actualmente dentro de ese mismo grupo, NO notificamos (como antes)
        val ctx = userPreferencesProvider.groupContextFlow.first()
        val isInActiveGroup = (ctx?.inGroup == true && ctx.groupName == groupName)

        if (isInActiveGroup) return

//        notificationHelper.showGroupNotification(
//            groupName = groupName,
//            unreadCount = payloadUnreadCount, // viene del push
//            lastSenderName = otherName,
//            lastMessage = content
//        )

    }
}
