package com.zibete.proyecto1

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getOtherUid
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.PayloadKeys
import com.zibete.proyecto1.core.utils.onFailure
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

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.w(TAG, "FCM received without data payload")
            return
        }

        Log.d(
            TAG,
            "FCM received type=${data[PayloadKeys.TYPE]} chatId=${data[PayloadKeys.CHAT_ID]} messageId=${data[PayloadKeys.MESSAGE_ID]}"
        )

        serviceScope.launch {
            try {
                val uid = authSessionProvider.currentUser?.uid
                if (uid.isNullOrBlank()) {
                    Log.w(TAG, "Skipping FCM: no authenticated user")
                    return@launch
                }

                handleDataMessage(data, uid)
            } catch (t: Throwable) {
                Log.e(TAG, "Error handling FCM", t)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return

        Log.d(TAG, "Refreshed FCM token received")

        serviceScope.launch {
            try {
                val uid = authSessionProvider.currentUser?.uid
                if (uid == null) {
                    Log.w(TAG, "Skipping FCM token sync: no authenticated user")
                    return@launch
                }

                val installId = sessionRepositoryProvider.getLocalInstallId()
                if (installId.isBlank()) {
                    Log.w(TAG, "Skipping FCM token sync: installId unavailable")
                    return@launch
                }

                sessionRepositoryActions.setActiveSession(
                    uid = uid,
                    installId = installId,
                    fcmToken = token
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Error syncing refreshed FCM token", t)
            }
        }
    }

    private suspend fun handleDataMessage(
        data: Map<String, String>,
        myUid: String
    ) {
        val nodeType = data[PayloadKeys.TYPE]
        if (nodeType.isNullOrBlank()) {
            Log.w(TAG, "Invalid FCM payload: missing type")
            return
        }

        // =========================
        // 1) CHAT 1-1 (NODE_DM)
        // =========================
        if (nodeType == NODE_DM) {
            handleDmMessage(data, myUid)
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

    private suspend fun handleDmMessage(
        data: Map<String, String>,
        myUid: String
    ) {
        val chatId = data[PayloadKeys.CHAT_ID]
        val messageId = data[PayloadKeys.MESSAGE_ID]
        if (chatId.isNullOrBlank() || messageId.isNullOrBlank()) {
            Log.w(TAG, "Invalid DM FCM payload: missing chatId or messageId")
            return
        }

        Log.d(TAG, "Valid DM FCM payload chatId=$chatId messageId=$messageId")

        val otherUid = getOtherUid(chatId, myUid)
        if (otherUid.isNullOrBlank()) {
            Log.w(TAG, "Invalid DM FCM payload: could not resolve otherUid chatId=$chatId")
            return
        }

        chatRepository.acknowledgeDmMessageReceived(
            myUid = myUid,
            otherUid = otherUid,
            nodeType = NODE_DM,
            messageId = messageId
        ).onFailure {
            Log.w(TAG, "Could not acknowledge DM message receipt", it)
        }

        val enabled = runCatching { userPreferencesProvider.individualNotificationsFlow.first() }
            .onFailure {
                Log.w(TAG, "Could not read DM notification preference; showing notification", it)
            }
            .getOrDefault(true)

        if (!enabled) {
            Log.i(TAG, "Skipping DM notification: individual notifications disabled chatId=$chatId")
            return
        }

        val fallbackSenderName = data[PayloadKeys.SENDER_NAME]
            ?.takeIf { it.isNotBlank() }
            ?: "ZIBE"
        val fallbackContent = data[PayloadKeys.CONTENT]
            ?.takeIf { it.isNotBlank() }
            ?: FALLBACK_DM_CONTENT

        val summary = runCatching {
            chatRepository.getUnreadSummaryForChats(myUid, NODE_DM)
        }.onFailure {
            Log.w(TAG, "Could not read DM unread summary; using fallback chatId=$chatId", it)
        }.getOrDefault(ChatRepository.UnreadSummary(totalChats = 1, totalUnread = 1))

        val conversation = runCatching {
            chatRepository.getConversation(
                firstUid = myUid,
                secondUid = otherUid,
                nodeType = NODE_DM
            )
        }.onFailure {
            Log.w(TAG, "Could not read DM conversation; using payload fallback chatId=$chatId", it)
        }.getOrNull()

        val senderName = conversation?.otherName
            ?.takeIf { it.isNotBlank() }
            ?: fallbackSenderName
        val content = conversation?.lastContent
            ?.takeIf { it.isNotBlank() }
            ?: fallbackContent

        Log.d(TAG, "Calling NotificationHelper for DM chatId=$chatId messageId=$messageId")
        notificationHelper.showChatSummaryNotification(
            summary = summary,
            lastSenderName = senderName,
            lastMessage = content,
            conversationId = chatId
        )
    }

    private companion object {
        const val TAG = "ZibeFCM"
        const val FALLBACK_DM_CONTENT = "Abri ZIBE para ver el mensaje"
    }
}
