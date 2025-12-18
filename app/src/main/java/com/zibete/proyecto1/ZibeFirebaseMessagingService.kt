package com.zibete.proyecto1

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesDSRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.notifications.NotificationHelper
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
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
        val novistos = data["novistos"].orEmpty()
        val userName = data["user"] ?: return
        val msg = data["msg"].orEmpty()
        val otherUid = data["id_user"] ?: return
        val type = data["type"] ?: return

        // type == NODE_CURRENT_CHAT => chat 1-1
        if (type == NODE_CURRENT_CHAT) {

            val enabled = userPreferencesDSRepository.individualNotificationsFlow.first()
            if (!enabled) {
                // igual marcamos visto (doble check) si corresponde
                chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, type)
                return
            }

            val summary = chatRepository.getUnreadSummaryForChats(myUid, type)

            if (summary.totalChats > 1) {
                val title = "${summary.totalUnread} mensajes de ${summary.totalChats} chats"
                val text = "$userName: $msg"
                notificationHelper.showMessageNotification(title, text)
            } else {
                val title = if (novistos == "1") "Nuevo mensaje de $userName" else "$novistos mensajes de $userName"
                notificationHelper.showMessageNotification(title, msg)
            }

            // marcar doble check (si aplica)
            chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, type)
            return
        }

        // Si no es NODE_CURRENT_CHAT, en tu payload "type" te queda como nombre de grupo
        val groupName = type

        val groupNotificationsEnabled = userPreferencesDSRepository.groupNotificationsFlow.first()
        if (!groupNotificationsEnabled) return

        val ctx = userPreferencesDSRepository.groupContextFlow.first()
        val isInActiveGroup = (ctx?.inGroup == true && ctx.groupName == groupName)

        if (!isInActiveGroup) {
            val title = "Nuevo mensaje de $groupName"
            val text = "$userName: $msg"
            notificationHelper.showMessageNotification(title, text)
        }
    }
}
