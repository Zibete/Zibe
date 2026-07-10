package com.zibete.proyecto1.data

import android.net.Uri
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
    val refOtherConversation: DatabaseReference
)

data class DeleteResult(
    val deletedCount: Int,
    val chatRemoved: Boolean
)

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
            refOtherConversation = refOtherConversation
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
            ?: return@zibeCatching

        if (message.senderUid != otherUid) return@zibeCatching

        setSeenAtLeast(messageRef.child(ChatMessageKeys.SEEN), MSG_RECEIVED)
        syncSenderConversationSeenIfLatest(
            senderUid = otherUid,
            receiverUid = myUid,
            nodeType = nodeType,
            messageCreatedAt = message.createdAt,
            targetSeen = MSG_RECEIVED
        )
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
        val refMyConversation = chatRefs.refMyConversation.get().await()

        markVisibleIncomingMessagesAsSeen(chatRefs)

        if (!refMyConversation.exists()) return@zibeCatching

        chatRefs.refMyConversation.child(ConversationKeys.SEEN).setValue(MSG_SEEN).await()
        chatRefs.refMyConversation.child(ConversationKeys.UNREAD_COUNT).setValue(0).await()
    }

    suspend fun markMessageAsSeenIfNeeded(
        chatRefs: ChatRefs,
        messageId: String,
        message: ChatMessage
    ): ZibeResult<Unit> = zibeCatching {
        markMessageAsSeenIfNeededInternal(chatRefs, messageId, message)
    }

    private suspend fun markVisibleIncomingMessagesAsSeen(chatRefs: ChatRefs) {
        val snapshot = chatRefs.refChat.get().await()

        if (!snapshot.exists()) return

        snapshot.children.forEach { child ->
            val messageId = child.key ?: return@forEach
            val message = child.getValue(ChatMessage::class.java) ?: return@forEach
            markMessageAsSeenIfNeededInternal(chatRefs, messageId, message)
        }
    }

    private suspend fun markMessageAsSeenIfNeededInternal(
        chatRefs: ChatRefs,
        messageId: String,
        message: ChatMessage
    ) {
        if (messageId.isBlank()) return
        if (message.senderUid == myUid) return
        if (message.seen >= MSG_SEEN) return
        if (message.isDeletedFor(myUid)) return

        chatRefs.refChat
            .child(messageId)
            .child(ChatMessageKeys.SEEN)
            .setValue(MSG_SEEN)
            .await()
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

    suspend fun applyDoubleCheckForLatestUnread(
        myUid: String,
        otherUid: String,
        nodeType: String
    ) {
        val conversationSeenRef = firebaseRefsContainer.refData
            .child(myUid)
            .child(nodeType)
            .child(otherUid)
            .child(ConversationKeys.SEEN)

        val currentConversationSeen =
            conversationSeenRef.get().await().getValue(Int::class.java) ?: 0

        if (currentConversationSeen < MSG_RECEIVED) {
            conversationSeenRef.setValue(MSG_RECEIVED).await()
        }

        val unSeenDs = firebaseRefsContainer.refData
            .child(myUid)
            .child(nodeType)
            .child(otherUid)
            .child(ConversationKeys.UNREAD_COUNT)
            .get()
            .await()

        val unSeen = unSeenDs.getValue(Int::class.java) ?: 0
        if (unSeen <= 0) return

        val chatId = getChatId(myUid, otherUid)

        val messagesDs = firebaseRefsContainer.refChatsRoot
            .child(nodeType)
            .child(chatId)
            .orderByChild(ChatMessageKeys.CREATED_AT)
            .limitToLast(unSeen)
            .get()
            .await()

        if (!messagesDs.exists()) return

        for (msgSnap in messagesDs.children) {
            val message = msgSnap.getValue(ChatMessage::class.java) ?: continue
            if (
                message.senderUid != myUid &&
                !message.isDeletedFor(myUid) &&
                message.seen < MSG_RECEIVED &&
                msgSnap.hasChild(ChatMessageKeys.SEEN)
            ) {
                msgSnap.ref.child(ChatMessageKeys.SEEN).setValue(MSG_RECEIVED).await()
            }
        }
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
}
