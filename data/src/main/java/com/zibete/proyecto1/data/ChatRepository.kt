package com.zibete.proyecto1.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getChatId
import com.zibete.proyecto1.core.constants.Constants.ChatMessageKeys
import com.zibete.proyecto1.core.constants.Constants.ConversationKeys
import com.zibete.proyecto1.core.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_RECEIVED
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.NODE_CHATS_ROOT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_USERS_DATA
import com.zibete.proyecto1.core.constants.Constants.NODE_USERS_ROOT
import com.zibete.proyecto1.core.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.core.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.isDeletedFor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ChatRefs(
    val refAudios: StorageReference,
    val refPhotos: StorageReference,
    val refChat: DatabaseReference,
    val refMyConversation: DatabaseReference,
    val refOtherConversation: DatabaseReference,
    val nodeType: String
)

data class DeleteResult(
    val deletedCount: Int,
    val chatRemoved: Boolean
)

private const val DM_SEEN_QUERY_BUFFER = 20

class ChatRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider,
) {

    val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    val myUid: String
        get() = firebaseUser.uid

    fun buildChatRefs(
        otherUid: String,
        nodeType: String
    ): ChatRefs {

        val chatId = getChatId(myUid, otherUid)

        val refAudios =
            firebaseRefsContainer.storageChatsRef
                .child("$nodeType/$chatId/")
                .child("$PATH_AUDIOS/")

        val refPhotos =
            firebaseRefsContainer.storageChatsRef
                .child("$nodeType/$chatId/")
                .child("$PATH_PHOTOS/")

        val refChat =
            firebaseRefsContainer.refChatsRoot
                .child(nodeType)
                .child(chatId)

        val refMyConversation =
            firebaseRefsContainer.refData
                .child(myUid)
                .child(nodeType)
                .child(otherUid)

        val refOtherConversation =
            firebaseRefsContainer.refData
                .child(otherUid)
                .child(nodeType)
                .child(myUid)

        return ChatRefs(
            refAudios = refAudios,
            refPhotos = refPhotos,
            refChat = refChat,
            refMyConversation = refMyConversation,
            refOtherConversation = refOtherConversation,
            nodeType = nodeType
        )
    }

    fun buildActiveChatKey(otherUid: String, nodeType: String): String {
        return "${otherUid}_${nodeType}"
    }

    fun observeChatMessages(chatRefs: ChatRefs): Flow<ChatChildEvent> = callbackFlow {
        val listener = object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.getValue(ChatMessage::class.java) ?: return
                trySend(ChatChildEvent.Added(ChatMessageItem(id, msg)))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.getValue(ChatMessage::class.java) ?: return
                trySend(ChatChildEvent.Changed(ChatMessageItem(id, msg)))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                trySend(ChatChildEvent.Removed(ChatMessageItem(id, ChatMessage())))
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }

        chatRefs.refChat.addChildEventListener(listener)
        awaitClose { chatRefs.refChat.removeEventListener(listener) }
    }

    suspend fun getConversation(
        firstUid: String = myUid,
        secondUid: String,
        nodeType: String
    ): Conversation? {
        val snapshot = firebaseRefsContainer.refData
            .child(firstUid)
            .child(nodeType)
            .child(secondUid)
            .get()
            .await()

        return snapshot.getValue(Conversation::class.java)
    }

    suspend fun hasConversation(
        otherUid: String,
        nodeType: String
    ): ZibeResult<Boolean> = zibeCatching {
        firebaseRefsContainer.refData
            .child(myUid)
            .child(nodeType)
            .child(otherUid)
            .get()
            .await()
            .exists()
    }

    suspend fun saveConversation(
        ownerUid: String,
        nodeType: String,
        otherUid: String,
        chatWith: Conversation
    ) {
        firebaseRefsContainer.refData
            .child(ownerUid)
            .child(nodeType)
            .child(otherUid)
            .setValue(chatWith)
            .await()
    }

    suspend fun pushMessageToChat(chatRefs: ChatRefs, message: ChatMessage) {
        chatRefs.refChat.push().setValue(message).await()
    }

    suspend fun sendDmMessageWithConversations(
        senderUid: String,
        receiverUid: String,
        message: ChatMessage,
        senderConversation: Conversation,
        receiverConversation: Conversation
    ): ZibeResult<Unit> = zibeCatching {
        val chatId = getChatId(senderUid, receiverUid)
        val messageId = checkNotNull(
            firebaseRefsContainer.refChatsDm.child(chatId).push().key
        ) { "Could not generate DM message id" }

        val updates = mapOf<String, Any>(
            "/$NODE_CHATS_ROOT/$NODE_DM/$chatId/$messageId" to message,
            "/$NODE_USERS_ROOT/$NODE_USERS_DATA/$senderUid/$NODE_DM/$receiverUid" to
                senderConversation,
            "/$NODE_USERS_ROOT/$NODE_USERS_DATA/$receiverUid/$NODE_DM/$senderUid" to
                receiverConversation
        )

        firebaseRefsContainer.firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun acknowledgeDmMessageReceived(
        myUid: String,
        otherUid: String,
        nodeType: String,
        messageId: String
    ): ZibeResult<Unit> = zibeCatching {
        if (nodeType != NODE_DM || messageId.isBlank()) return@zibeCatching

        val chatId = getChatId(myUid, otherUid)
        val messageRef = firebaseRefsContainer.refChatsDm
            .child(chatId)
            .child(messageId)
        val message = messageRef.get().await().getValue(ChatMessage::class.java)
        if (message == null) {
            Log.w(TAG, "DM receipt ack skipped: message not found id=${safeId(messageId)}")
            return@zibeCatching
        }

        if (message.senderUid != otherUid) {
            Log.w(TAG, "DM receipt ack skipped: sender mismatch id=${safeId(messageId)}")
            return@zibeCatching
        }

        setSeenAtLeast(messageRef.child(ChatMessageKeys.SEEN), MSG_RECEIVED)
        syncSenderConversationSeenIfLatest(
            senderUid = otherUid,
            receiverUid = myUid,
            nodeType = nodeType,
            messageCreatedAt = message.createdAt,
            targetSeen = MSG_RECEIVED
        )
        Log.d(TAG, "DM receipt ack completed id=${safeId(messageId)} target=$MSG_RECEIVED")
    }

    suspend fun uploadMedia(
        uri: Uri,
        fileName: String,
        refData: StorageReference
    ): String? {
        return try {
            val fileRef = refData.child(fileName)
            fileRef.putFile(uri).await()
            fileRef.downloadUrl.await().toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun markChatAsSeen(chatRefs: ChatRefs): ZibeResult<Unit> = zibeCatching {
        if (chatRefs.nodeType != NODE_DM) {
            markLegacyChatAsSeen(chatRefs)
            return@zibeCatching
        }

        val myConversation = chatRefs.refMyConversation.get().await()
        if (!myConversation.exists()) return@zibeCatching

        val unreadCount = myConversation
            .child(ConversationKeys.UNREAD_COUNT)
            .getValue(Int::class.java) ?: 0
        markPendingIncomingMessagesAsSeen(chatRefs, unreadCount)

        chatRefs.refMyConversation.updateChildren(
            mapOf(
                ConversationKeys.SEEN to MSG_SEEN,
                ConversationKeys.UNREAD_COUNT to 0
            )
        ).await()
        Log.d(
            TAG,
            "DM unread cleared chat=${safeId(chatRefs.refChat.key)} pending=${unreadCount.coerceAtLeast(0)}"
        )
    }

    suspend fun markMessageAsSeenIfNeeded(
        chatRefs: ChatRefs,
        messageId: String,
        message: ChatMessage
    ): ZibeResult<Unit> = zibeCatching {
        markMessageAsSeenIfNeededInternal(chatRefs, messageId, message)
    }

    private suspend fun markPendingIncomingMessagesAsSeen(
        chatRefs: ChatRefs,
        unreadCount: Int
    ) {
        val queryLimit = (unreadCount.coerceAtLeast(1).toLong() + DM_SEEN_QUERY_BUFFER)
            .coerceAtMost(MAX_CHAT_SIZE.toLong())
            .toInt()
        val snapshot = chatRefs.refChat
            .orderByChild(ChatMessageKeys.CREATED_AT)
            .limitToLast(queryLimit)
            .get()
            .await()

        if (!snapshot.exists()) return

        val pendingMessages = snapshot.children.mapNotNull { child ->
            val messageId = child.key ?: return@mapNotNull null
            val message = child.getValue(ChatMessage::class.java) ?: return@mapNotNull null
            if (
                message.senderUid == myUid ||
                message.seen >= MSG_SEEN ||
                message.isDeletedFor(myUid)
            ) {
                return@mapNotNull null
            }
            ChatMessageItem(messageId, message)
        }

        if (pendingMessages.isEmpty()) return

        chatRefs.refChat.updateChildren(
            pendingMessages.associate { item ->
                "${item.id}/${ChatMessageKeys.SEEN}" to MSG_SEEN
            }
        ).await()

        val latestMessage = pendingMessages.maxBy { it.message.createdAt }.message
        syncSenderConversationSeenIfLatest(
            conversationRef = chatRefs.refOtherConversation,
            senderUid = latestMessage.senderUid,
            messageCreatedAt = latestMessage.createdAt,
            targetSeen = MSG_SEEN
        )
    }

    private suspend fun markLegacyChatAsSeen(chatRefs: ChatRefs) {
        val myConversation = chatRefs.refMyConversation.get().await()
        val snapshot = chatRefs.refChat.get().await()

        snapshot.children.forEach { child ->
            val messageId = child.key ?: return@forEach
            val message = child.getValue(ChatMessage::class.java) ?: return@forEach
            markIncomingMessageSeenAndSyncSender(chatRefs, messageId, message)
        }

        if (!myConversation.exists()) return
        setSeenAtLeast(chatRefs.refMyConversation.child(ConversationKeys.SEEN), MSG_SEEN)
        clearMyUnreadCount(chatRefs)
    }

    private suspend fun markMessageAsSeenIfNeededInternal(
        chatRefs: ChatRefs,
        messageId: String,
        message: ChatMessage
    ) {
        if (messageId.isBlank()) return
        if (message.senderUid == myUid) return
        if (message.seen >= MSG_SEEN) {
            Log.d(TAG, "Seen write skipped: already seen id=${safeId(messageId)}")
            return
        }
        if (message.isDeletedFor(myUid)) return

        markIncomingMessageSeenAndSyncSender(chatRefs, messageId, message)
        clearMyUnreadCount(chatRefs)
    }

    private suspend fun markIncomingMessageSeenAndSyncSender(
        chatRefs: ChatRefs,
        messageId: String,
        message: ChatMessage
    ) {
        if (messageId.isBlank()) return
        if (message.senderUid == myUid) return
        if (message.seen >= MSG_SEEN) return
        if (message.isDeletedFor(myUid)) return

        setMessageSeenAtLeast(chatRefs, messageId, MSG_SEEN)
        syncSenderConversationSeenIfLatest(
            conversationRef = chatRefs.refOtherConversation,
            senderUid = message.senderUid,
            messageCreatedAt = message.createdAt,
            targetSeen = MSG_SEEN
        )
    }

    private suspend fun setMessageSeenAtLeast(
        chatRefs: ChatRefs,
        messageId: String,
        targetSeen: Int
    ) {
        setSeenAtLeast(
            chatRefs.refChat.child(messageId).child(ChatMessageKeys.SEEN),
            targetSeen
        )
    }

    private suspend fun clearMyUnreadCount(chatRefs: ChatRefs) {
        chatRefs.refMyConversation.runTransactionAwait { currentData ->
            if (currentData.value != null) {
                currentData.child(ConversationKeys.UNREAD_COUNT).value = 0
            }
            Transaction.success(currentData)
        }
        Log.d(TAG, "Unread count cleared chat=${safeId(chatRefs.refChat.key)}")
    }

    suspend fun deleteMessages(
        chatRefs: ChatRefs,
        selectedIds: List<String>?
    ): ZibeResult<DeleteResult> = zibeCatching {
        if (selectedIds == null) {
            return@zibeCatching deleteConversationForMeInternal(chatRefs)
        } else {

            // 2) Determinar ids a procesar
            val idsToProcess: List<String> = selectedIds

            // 3) Procesar cada mensaje por id
            var processed = 0
            for (id in idsToProcess) {
                val msgSnap = chatRefs.refChat.child(id).get().await()
                if (!msgSnap.exists()) continue

                val type = msgSnap.child(ChatMessageKeys.TYPE).getValue(Int::class.java) ?: continue
                val senderUid =
                    msgSnap.child(ChatMessageKeys.SENDER_UID).getValue(String::class.java)
                        ?: continue
                val content =
                    msgSnap.child(ChatMessageKeys.CONTENT).getValue(String::class.java).orEmpty()

                processSoftDeleteOrRemove(
                    msgRef = msgSnap.ref,
                    type = type,
                    senderUid = senderUid,
                    content = content
                )
                processed++
            }

            // 4) Si quedó vacío (o all eliminado para mí), borrar conversación
            val chatRemoved = removeConversationIfEmpty(chatRefs)

            DeleteResult(
                deletedCount = processed,
                chatRemoved = chatRemoved
            )
        }
    }

    suspend fun deleteConversationForMe(chatRefs: ChatRefs): ZibeResult<DeleteResult> =
        zibeCatching { deleteConversationForMeInternal(chatRefs) }

    private suspend fun deleteConversationForMeInternal(chatRefs: ChatRefs): DeleteResult {
        val snapshot = chatRefs.refChat.get().await()

        if (!snapshot.exists() || snapshot.childrenCount == 0L) {
            chatRefs.refMyConversation.removeValue().await()
            return DeleteResult(
                deletedCount = 0,
                chatRemoved = true
            )
        }

        var processed = 0
        snapshot.children.forEach { child ->
            val message = child.getValue(ChatMessage::class.java) ?: return@forEach
            if (message.isDeletedFor(myUid)) return@forEach

            processSoftDeleteOrRemove(
                msgRef = child.ref,
                type = message.type,
                senderUid = message.senderUid,
                content = message.content
            )
            processed++
        }

        val chatRemoved = removeConversationIfEmpty(chatRefs)

        return DeleteResult(
            deletedCount = processed,
            chatRemoved = chatRemoved
        )
    }

    private suspend fun processSoftDeleteOrRemove(
        msgRef: DatabaseReference,
        type: Int,
        senderUid: String,
        content: String
    ) {

        if (senderUid == myUid) {
            when (type) {
                MSG_TEXT -> msgRef.child(ChatMessageKeys.TYPE).setValue(MSG_TEXT_SENDER_DLT).await()
                MSG_PHOTO -> msgRef.child(ChatMessageKeys.TYPE).setValue(MSG_PHOTO_SENDER_DLT)
                    .await()

                MSG_AUDIO -> msgRef.child(ChatMessageKeys.TYPE).setValue(MSG_AUDIO_SENDER_DLT)
                    .await()

                MSG_TEXT_RECEIVER_DLT -> msgRef.removeValue().await()

                MSG_PHOTO_RECEIVER_DLT -> {
                    deleteStorageByUrlIfPossible(content)
                    msgRef.removeValue().await()
                }

                MSG_AUDIO_RECEIVER_DLT -> {
                    deleteStorageByUrlIfPossible(content)
                    msgRef.removeValue().await()
                }
            }
        } else {
            when (type) {
                MSG_TEXT -> msgRef.child(ChatMessageKeys.TYPE).setValue(MSG_TEXT_RECEIVER_DLT)
                    .await()

                MSG_PHOTO -> msgRef.child(ChatMessageKeys.TYPE).setValue(MSG_PHOTO_RECEIVER_DLT)
                    .await()

                MSG_AUDIO -> msgRef.child(ChatMessageKeys.TYPE).setValue(MSG_AUDIO_RECEIVER_DLT)
                    .await()

                MSG_TEXT_SENDER_DLT -> msgRef.removeValue().await()

                MSG_PHOTO_SENDER_DLT -> {
                    deleteStorageByUrlIfPossible(content)
                    msgRef.removeValue().await()
                }

                MSG_AUDIO_SENDER_DLT -> {
                    deleteStorageByUrlIfPossible(content)
                    msgRef.removeValue().await()
                }
            }
        }
    }

    private suspend fun deleteStorageByUrlIfPossible(url: String) {
        if (url.isBlank()) return
        try {
            firebaseRefsContainer.firebaseStorage.getReferenceFromUrl(url).delete().await()
        } catch (_: Exception) {
            // ignore: no existe / no es storage / permisos / etc.
        }
    }


    suspend fun removeConversationIfEmpty(chatRefs: ChatRefs): Boolean {
        val snapshot = chatRefs.refChat.get().await()
        val total = snapshot.childrenCount.toInt()

        if (total == 0) {
            chatRefs.refMyConversation.removeValue().await()
            return true
        }

        var visibleCount = 0
        snapshot.children.forEach { child ->
            val message = child.getValue(ChatMessage::class.java) ?: return@forEach
            if (!message.isDeletedFor(myUid)) {
                visibleCount++
            }
        }

        return if (visibleCount == 0) {
            chatRefs.refMyConversation.removeValue().await()
            true
        } else {
            false
        }
    }

    suspend fun getMessageCount(chatRefs: ChatRefs): Int {
        return chatRefs.refChat.get().await().childrenCount.toInt()
    }

    // -----------------------------------------------------
    // Conversation
    data class UnreadSummary(
        val totalChats: Int,
        val totalUnread: Int
    )

    suspend fun getUnreadSummaryForChats(
        myUid: String,
        nodeType: String
    ): UnreadSummary {

        val ds = firebaseRefsContainer.refData
            .child(myUid)
            .child(nodeType)
            .orderByChild(ConversationKeys.UNREAD_COUNT)
            .startAt(1.0)
            .get()
            .await()

        if (!ds.exists()) return UnreadSummary(0, 0)

        var totalUnread = 0
        val totalChats = ds.childrenCount.toInt()

        for (child in ds.children) {
            totalUnread += child.child(ConversationKeys.UNREAD_COUNT).getValue(Int::class.java) ?: 0
        }

        return UnreadSummary(totalChats, totalUnread)
    }

    private suspend fun setSeenAtLeast(
        seenRef: DatabaseReference,
        targetSeen: Int
    ) {
        seenRef.runTransactionAwait { currentData ->
            val currentSeen = (currentData.value as? Number)?.toInt() ?: 0
            if (currentSeen < targetSeen) currentData.value = targetSeen
            Transaction.success(currentData)
        }
    }

    private suspend fun syncSenderConversationSeenIfLatest(
        senderUid: String,
        receiverUid: String,
        nodeType: String,
        messageCreatedAt: Long,
        targetSeen: Int
    ) {
        val conversationRef = firebaseRefsContainer.refData
            .child(senderUid)
            .child(nodeType)
            .child(receiverUid)
        syncSenderConversationSeenIfLatest(
            conversationRef = conversationRef,
            senderUid = senderUid,
            messageCreatedAt = messageCreatedAt,
            targetSeen = targetSeen
        )
    }

    private suspend fun syncSenderConversationSeenIfLatest(
        conversationRef: DatabaseReference,
        senderUid: String,
        messageCreatedAt: Long,
        targetSeen: Int
    ) {
        conversationRef.runTransactionAwait { currentData ->
            val currentSenderUid = currentData.child(ConversationKeys.USER_ID).value as? String
            val currentLastMessageAt =
                (currentData.child(ConversationKeys.LAST_MESSAGE_AT).value as? Number)?.toLong()
            val currentSeen =
                (currentData.child(ConversationKeys.SEEN).value as? Number)?.toInt() ?: 0

            if (
                currentSenderUid == senderUid &&
                currentLastMessageAt == messageCreatedAt &&
                currentSeen < targetSeen
            ) {
                currentData.child(ConversationKeys.SEEN).value = targetSeen
            }
            Transaction.success(currentData)
        }
    }

    private suspend fun DatabaseReference.runTransactionAwait(
        update: (MutableData) -> Transaction.Result
    ) {
        suspendCancellableCoroutine { continuation ->
            runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result =
                    update(currentData)

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (!continuation.isActive) return
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }
    }

    private fun safeId(value: String?): String {
        if (value.isNullOrBlank()) return "missing"
        if (value.length <= 8) return "${value.take(2)}..."
        return "${value.take(4)}...${value.takeLast(3)}"
    }

    private companion object {
        const val TAG = "ZibeDmSeen"
    }
}
