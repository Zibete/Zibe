package com.zibete.proyecto1

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getOtherUid
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.PayloadKeys
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ZibeFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authSessionProvider: AuthSessionProvider
    @Inject lateinit var userPreferencesProvider: UserPreferencesProvider
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var sessionRepositoryActions: SessionRepositoryActions
    @Inject lateinit var sessionRepositoryProvider: SessionRepositoryProvider
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    val myUid: String
        get() = firebaseUser.uid

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) return

        serviceScope.launch {
            try {
                handleDataMessage(data, myUid)
            } catch (t: Throwable) {
                Log.e("ZibeFCM", "Error handling FCM", t)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return

        Log.d("ZibeFCM", "Refreshed FCM token received")

        serviceScope.launch {
            try {
                val uid = authSessionProvider.currentUser?.uid
                if (uid == null) {
                    Log.w("ZibeFCM", "Skipping FCM token sync: no authenticated user")
                    return@launch
                }

                val installId = sessionRepositoryProvider.getLocalInstallId()
                if (installId.isBlank()) {
                    Log.w("ZibeFCM", "Skipping FCM token sync: installId unavailable")
                    return@launch
                }

                sessionRepositoryActions.setActiveSession(
                    uid = uid,
                    installId = installId,
                    fcmToken = token
                )
            } catch (t: Throwable) {
                Log.e("ZibeFCM", "Error syncing refreshed FCM token", t)
            }
        }
    }

    private suspend fun handleDataMessage(
        data: Map<String, String>,
        myUid: String
    ) {
        val nodeType = data[PayloadKeys.TYPE]
        if (nodeType.isNullOrBlank()) {
            Log.w("ZibeFCM", "Invalid FCM payload: missing type")
            return
        }

        // =========================
        // 1) CHAT 1-1 (NODE_DM)
        // =========================
        if (nodeType == NODE_DM) {
            val chatId = data[PayloadKeys.CHAT_ID]
            val messageId = data[PayloadKeys.MESSAGE_ID]
            if (chatId.isNullOrBlank() || messageId.isNullOrBlank()) {
                Log.w("ZibeFCM", "Invalid DM FCM payload: missing chatId or messageId")
                return
            }

            val otherUid = getOtherUid(chatId, myUid)
            if (otherUid.isNullOrBlank()) {
                Log.w("ZibeFCM", "Invalid DM FCM payload: could not resolve otherUid")
                return
            }

            val enabled = userPreferencesProvider.individualNotificationsFlow.first()
            if (!enabled) {
                chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, nodeType)
                return
            }

            val summary = chatRepository.getUnreadSummaryForChats(myUid, nodeType)
            val conversation = chatRepository.getConversation(
                firstUid = myUid,
                secondUid = otherUid,
                nodeType = nodeType
            )
            val otherName = conversation?.otherName?.takeIf { it.isNotBlank() } ?: otherUid
            val lastMessage = conversation?.lastContent
                ?.takeIf { it.isNotBlank() }
                ?: "Abrí ZIBE para ver el mensaje"

            notificationHelper.showChatSummaryNotification(
                summary = summary,
                lastSenderName = otherName,
                lastMessage = lastMessage,
                conversationId = chatId
            )

            chatRepository.applyDoubleCheckForLatestUnread(myUid, otherUid, nodeType)
            return
        }

        // =========================
        // 2) GRUPO (type = groupName en tu payload)
        // =========================
        val groupName = nodeType

        val groupEnabled = userPreferencesProvider.groupNotificationsFlow.first()
        if (!groupEnabled) return

        val ctx = userPreferencesProvider.groupContextFlow.first()
        val isInActiveGroup = (ctx?.inGroup == true && ctx.groupName == groupName)

        if (isInActiveGroup) return

//        notificationHelper.showGroupNotification(
//            groupName = groupName,
//            unreadCount = data[PayloadKeys.UNREAD_COUNT].orEmpty().toInt(), // viene del push
//            lastSenderName = data[PayloadKeys.OTHER_NAME] ?: return,
//            lastMessage = data[PayloadKeys.CONTENT].orEmpty()
//        )
    }
}
