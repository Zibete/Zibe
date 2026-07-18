package com.zibete.proyecto1

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getOtherUid
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_ROOM
import com.zibete.proyecto1.core.constants.Constants.PayloadKeys
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.runCatchingPreservingCancellation
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.DirectMessageReceiptAcknowledger
import com.zibete.proyecto1.data.UnreadSummary
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

@AndroidEntryPoint
class ZibeFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authSessionProvider: AuthSessionProvider
    @Inject lateinit var userPreferencesProvider: UserPreferencesProvider
    @Inject lateinit var chatRepository: ChatRepositoryContract
    @Inject lateinit var receiptAcknowledger: DirectMessageReceiptAcknowledger
    @Inject lateinit var sessionRepositoryActions: SessionRepositoryActions
    @Inject lateinit var sessionRepositoryProvider: SessionRepositoryProvider
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.w(TAG, "FCM received without data payload")
            return
        }

        val completed = runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(CALLBACK_TIMEOUT_MS) {
                try {
                    val uid = authSessionProvider.currentUser?.uid
                    if (uid.isNullOrBlank()) {
                        Log.w(TAG, "Skipping FCM: no authenticated user")
                        return@withTimeoutOrNull true
                    }

                    handleDataMessage(data, uid)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.e(TAG, "Error handling FCM", exception)
                }
                true
            } ?: false
        }
        if (!completed) {
            Log.w(TAG, "FCM callback timed out; showing payload fallback")
            showPayloadFallback(data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return

        Log.d(TAG, "Refreshed FCM token received")

        runBlocking(Dispatchers.IO) {
            val completed = withTimeoutOrNull(CALLBACK_TIMEOUT_MS) {
                try {
                    val uid = authSessionProvider.currentUser?.uid
                    if (uid == null) {
                        Log.w(TAG, "Skipping FCM token sync: no authenticated user")
                        return@withTimeoutOrNull true
                    }

                    val installId = sessionRepositoryProvider.getLocalInstallId()
                    if (installId.isBlank()) {
                        Log.w(TAG, "Skipping FCM token sync: installId unavailable")
                        return@withTimeoutOrNull true
                    }

                    sessionRepositoryActions.setActiveSession(uid, installId, token)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.e(TAG, "Error syncing refreshed FCM token", exception)
                }
                true
            } ?: false
            if (!completed) {
                Log.w(TAG, "FCM token callback timed out; bootstrap will retry session sync")
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

        handleRoomMessage(data, nodeType)
    }

    private suspend fun handleRoomMessage(data: Map<String, String>, nodeType: String) {
        val roomKey = data[PayloadKeys.ROOM_KEY]
            ?.takeIf(String::isNotBlank)
            ?: nodeType.takeIf { it != NODE_ROOM && it.isNotBlank() }
        if (roomKey.isNullOrBlank()) {
            Log.w(TAG, "Invalid room FCM payload: missing roomKey")
            return
        }
        val messageId = data[PayloadKeys.MESSAGE_ID]?.takeIf(String::isNotBlank)
        if (messageId == null) {
            Log.w(TAG, "Invalid room FCM payload: missing messageId")
            return
        }

        val enabled = runCatchingPreservingCancellation {
            userPreferencesProvider.groupNotificationsFlow.first()
        }.onFailure {
            Log.w(TAG, "Could not read room notification preference; skipping notification", it)
        }.getOrDefault(false)
        if (!enabled) {
            Log.i(TAG, "Skipping room notification: group notifications disabled")
            return
        }

        val roomName = data[PayloadKeys.ROOM_NAME]
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.menu_groups)
        val senderName = data[PayloadKeys.SENDER_NAME]
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.app_name)
        val preview = data[PayloadKeys.PREVIEW]
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.rooms_notification_media_fallback)

        notificationHelper.showRoomNotification(
            roomKey = roomKey,
            messageId = messageId,
            roomName = roomName,
            lastSenderName = senderName,
            lastMessage = preview
        )
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

        val otherUid = getOtherUid(chatId, myUid)
        if (otherUid.isNullOrBlank()) {
            Log.w(
                TAG,
                "Invalid DM FCM payload: could not resolve otherUid chatId=${safeId(chatId)}"
            )
            return
        }

        receiptAcknowledger.acknowledgeReceived(
            myUid = myUid,
            otherUid = otherUid,
            messageId = messageId
        ).onFailure {
            Log.w(TAG, "Could not acknowledge DM message receipt", it)
        }

        val enabled = runCatchingPreservingCancellation {
            userPreferencesProvider.individualNotificationsFlow.first()
        }
            .onFailure {
                Log.w(TAG, "Could not read DM notification preference; showing notification", it)
            }
            .getOrDefault(true)

        if (!enabled) {
            Log.i(
                TAG,
                "Skipping DM notification: individual notifications disabled " +
                    "chatId=${safeId(chatId)}"
            )
            return
        }

        val fallbackSenderName = data[PayloadKeys.SENDER_NAME]
            ?.takeIf { it.isNotBlank() }
            ?: "ZIBE"
        val fallbackContent = data[PayloadKeys.CONTENT]
            ?.takeIf { it.isNotBlank() }
            ?: FALLBACK_DM_CONTENT

        val summary = runCatchingPreservingCancellation {
            chatRepository.getUnreadSummaryForChats(myUid, NODE_DM)
        }.onFailure {
            Log.w(
                TAG,
                "Could not read DM unread summary; using fallback chatId=${safeId(chatId)}",
                it
            )
        }.getOrDefault(UnreadSummary(totalChats = 1, totalUnread = 1))

        val conversation = runCatchingPreservingCancellation {
            chatRepository.getConversation(
                firstUid = myUid,
                secondUid = otherUid,
                nodeType = NODE_DM
            )
        }.onFailure {
            Log.w(
                TAG,
                "Could not read DM conversation; using payload fallback chatId=${safeId(chatId)}",
                it
            )
        }.getOrNull()

        val senderName = conversation?.otherName
            ?.takeIf { it.isNotBlank() }
            ?: fallbackSenderName
        val content = conversation?.lastContent
            ?.takeIf { it.isNotBlank() }
            ?: fallbackContent

        notificationHelper.showChatSummaryNotification(
            summary = summary,
            lastSenderName = senderName,
            lastMessage = content,
            conversationId = chatId
        )
    }

    private fun showPayloadFallback(data: Map<String, String>) {
        if (data[PayloadKeys.TYPE] == NODE_DM) {
            val chatId = data[PayloadKeys.CHAT_ID]?.takeIf(String::isNotBlank) ?: return
            notificationHelper.showChatSummaryNotification(
                summary = UnreadSummary(totalChats = 1, totalUnread = 1),
                lastSenderName = data[PayloadKeys.SENDER_NAME]
                    ?.takeIf(String::isNotBlank)
                    ?: "ZIBE",
                lastMessage = data[PayloadKeys.CONTENT]
                    ?.takeIf(String::isNotBlank)
                    ?: FALLBACK_DM_CONTENT,
                conversationId = chatId
            )
            return
        }

        val roomKey = data[PayloadKeys.ROOM_KEY]?.takeIf(String::isNotBlank) ?: return
        val messageId = data[PayloadKeys.MESSAGE_ID]?.takeIf(String::isNotBlank) ?: return
        val notificationsEnabled = runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(FALLBACK_PREFERENCE_TIMEOUT_MS) {
                userPreferencesProvider.groupNotificationsFlow.first()
            }
        } ?: false
        if (!notificationsEnabled) {
            Log.w(TAG, "Skipping room fallback notification: preference unavailable or disabled")
            return
        }
        notificationHelper.showRoomNotification(
            roomKey = roomKey,
            messageId = messageId,
            roomName = data[PayloadKeys.ROOM_NAME]?.takeIf(String::isNotBlank)
                ?: getString(R.string.menu_groups),
            lastSenderName = data[PayloadKeys.SENDER_NAME]?.takeIf(String::isNotBlank)
                ?: getString(R.string.app_name),
            lastMessage = data[PayloadKeys.PREVIEW]?.takeIf(String::isNotBlank)
                ?: getString(R.string.rooms_notification_media_fallback)
        )
    }

    private companion object {
        const val TAG = "ZibeFCM"
        const val FALLBACK_DM_CONTENT = "Abri ZIBE para ver el mensaje"
        const val CALLBACK_TIMEOUT_MS = 8_000L
        const val FALLBACK_PREFERENCE_TIMEOUT_MS = 500L

        fun safeId(value: String?): String {
            if (value.isNullOrBlank()) return "missing"
            if (value.length <= 8) return "${value.take(2)}..."
            return "${value.take(4)}...${value.takeLast(3)}"
        }
    }
}
