package com.zibete.proyecto1.data

import android.net.Uri
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.ui.constants.Constants.ChatKeys
import com.zibete.proyecto1.ui.constants.Constants.ChatListKeys
import com.zibete.proyecto1.ui.constants.Constants.ConversationKeys
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_RECEIVED
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS_ROOT
import com.zibete.proyecto1.ui.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.ui.constants.Constants.PATH_PHOTOS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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

// ✅ Nuevo: events con item (incluye id)
sealed class ChatChildEvent {
    data class Added(val item: ChatMessageItem) : ChatChildEvent()
    data class Changed(val item: ChatMessageItem) : ChatChildEvent()
    data class Removed(val item: ChatMessageItem) : ChatChildEvent()
}

class ChatRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {
    private val myUid: String get() = userRepository.myUid

    fun buildChatRefs(
        otherUid: String,
        nodeType: String
    ): ChatRefs {

        val chatId = getChatId(otherUid)

        val refAudios =
            firebaseRefsContainer.firebaseStorage.reference
                .child("$NODE_CHATS_ROOT/$nodeType/$chatId/")
                .child("$PATH_AUDIOS/")

        val refPhotos =
            firebaseRefsContainer.firebaseStorage.reference
                .child("$NODE_CHATS_ROOT/$nodeType/$chatId/")
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

    fun getChatId(otherUid: String): String {
        val (first, second) = listOf(myUid, otherUid).sorted()
        return "${first}_${second}"
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
        firstUid: String,
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

    suspend fun markChatAsSeen(chatRefs: ChatRefs) {
        val refMyConversation = chatRefs.refMyConversation.get().await()
        if (!refMyConversation.exists()) return

        val unReadCount = refMyConversation
            .child(ConversationKeys.UNREAD_COUNT)
            .getValue(Int::class.java) ?: 0

        if (unReadCount > 0) setDoubleCheckOnLastMessages(chatRefs, unReadCount)

        chatRefs.refMyConversation.child(ConversationKeys.SEEN).setValue(MSG_SEEN)
        chatRefs.refMyConversation.child(ConversationKeys.UNREAD_COUNT).setValue(0)
    }

    private suspend fun setDoubleCheckOnLastMessages(
        chatRefs: ChatRefs,
        unReadCount: Int
    ) {
        val snapshot = chatRefs.refChat
            .orderByChild(ChatKeys.DATE)
            .limitToLast(unReadCount)
            .get()
            .await()

        if (!snapshot.exists()) return

        for (snap in snapshot.children) {
            if (snap.hasChild(ChatKeys.SEEN)) {
                snap.ref.child(ChatKeys.SEEN).setValue(MSG_SEEN)
            }
        }
    }

    suspend fun deleteMessages(
        chatRefs: ChatRefs,
        selectedIds: List<String>?,
        deleteMessages: Boolean = true
    ): DeleteResult {

        // 0) Si es "ocultar chat" y no hay selección: solo ocultar conversación (no toca mensajes)
        if (!deleteMessages && selectedIds == null) {
            chatRefs.refMyConversation
                .child(ConversationKeys.STATE)
                .setValue(CHAT_STATE_HIDE)
                .await()

            return DeleteResult(deletedCount = 0, chatRemoved = false)
        }

        // 1) Determinar ids a procesar
        val idsToProcess: List<String> = selectedIds ?: run {
            val snap = chatRefs.refChat.get().await()
            snap.children.mapNotNull { it.key }
        }

        // 2) Procesar cada mensaje por id
        var processed = 0
        for (id in idsToProcess) {
            val msgSnap = chatRefs.refChat.child(id).get().await()
            if (!msgSnap.exists()) continue

            val type = msgSnap.child(ChatKeys.TYPE).getValue(Int::class.java) ?: continue
            val senderUid =
                msgSnap.child(ChatKeys.SENDER_UID).getValue(String::class.java) ?: continue
            val content = msgSnap.child(ChatKeys.CONTENT).getValue(String::class.java).orEmpty()

            processSoftDeleteOrRemove(
                chatRefs = chatRefs,
                msgRef = msgSnap.ref,
                type = type,
                senderUid = senderUid,
                content = content
            )
            processed++
        }

        // 3) Si quedó vacío (o all eliminado para mí), borrar conversación
        val chatRemoved = removeConversationIfEmpty(chatRefs)

        return DeleteResult(
            deletedCount = processed,
            chatRemoved = chatRemoved
        )
    }

    // ✅ Reemplaza iterateDelete: ya no iteramos por query, procesamos por msgRef directo
    private suspend fun processSoftDeleteOrRemove(
        chatRefs: ChatRefs,
        msgRef: DatabaseReference,
        type: Int,
        senderUid: String,
        content: String
    ) {

        if (senderUid == myUid) {
            when (type) {
                MSG_TEXT -> msgRef.child(ChatKeys.TYPE).setValue(MSG_TEXT_SENDER_DLT).await()
                MSG_PHOTO -> msgRef.child(ChatKeys.TYPE).setValue(MSG_PHOTO_SENDER_DLT).await()
                MSG_AUDIO -> msgRef.child(ChatKeys.TYPE).setValue(MSG_AUDIO_SENDER_DLT).await()

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
                MSG_TEXT -> msgRef.child(ChatKeys.TYPE).setValue(MSG_TEXT_RECEIVER_DLT).await()
                MSG_PHOTO -> msgRef.child(ChatKeys.TYPE).setValue(MSG_PHOTO_RECEIVER_DLT).await()
                MSG_AUDIO -> msgRef.child(ChatKeys.TYPE).setValue(MSG_AUDIO_RECEIVER_DLT).await()

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

        var deletedCount = 0
        snapshot.children.forEach { child ->
            val type = child.child(ChatKeys.TYPE).getValue(Int::class.java) ?: return@forEach
            val senderUid =
                child.child(ChatKeys.SENDER_UID).getValue(String::class.java) ?: return@forEach

            if (senderUid == myUid) {
                when (type) {
                    MSG_TEXT_SENDER_DLT,
                    MSG_PHOTO_SENDER_DLT,
                    MSG_AUDIO_SENDER_DLT -> deletedCount++
                }
            } else {
                when (type) {
                    MSG_TEXT_RECEIVER_DLT,
                    MSG_PHOTO_RECEIVER_DLT,
                    MSG_AUDIO_RECEIVER_DLT -> deletedCount++
                }
            }
        }

        return if (total == deletedCount) {
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
        firebaseRefsContainer.refData
            .child(myUid)
            .child(nodeType)
            .child(otherUid)
            .child(ConversationKeys.SEEN)
            .setValue(MSG_RECEIVED)
            .await()

        val unSeenDs = firebaseRefsContainer.refData
            .child(myUid)
            .child(nodeType)
            .child(otherUid)
            .child(ConversationKeys.UNREAD_COUNT)
            .get()
            .await()

        val unSeen = unSeenDs.getValue(Int::class.java) ?: 0
        if (unSeen <= 0) return

        val chatId = getChatId(otherUid)

        val messagesDs = firebaseRefsContainer.refChatsRoot
            .child(nodeType)
            .child(chatId)
            .orderByChild(ChatKeys.DATE)
            .limitToLast(unSeen)
            .get()
            .await()

        if (!messagesDs.exists()) return

        for (msgSnap in messagesDs.children) {
            val sender = msgSnap.child(ChatKeys.SENDER_UID).getValue(String::class.java)
            if (sender != null && sender != myUid && msgSnap.hasChild(ChatKeys.SEEN)) {
                msgSnap.ref.child(ChatKeys.SEEN).setValue(MSG_RECEIVED).await()
            }
        }
    }
}
