package com.zibete.proyecto1.data

import android.net.Uri
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_RECEIVED_UNREAD
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN_STATUS
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.NODE_ACTIVE_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS
import com.zibete.proyecto1.ui.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.ui.constants.Constants.PATH_PHOTOS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatRefs(
    val refAudios: StorageReference,
    val refPhotos:StorageReference,
    val refChat: DatabaseReference,
    val refMyActiveChat: DatabaseReference,
    val refOtherActiveChat: DatabaseReference,
    val refMyChatListItem: DatabaseReference,
    val refOtherChatListItem: DatabaseReference
)

data class DeleteResult(
    val deletedCount: Int,
    val chatRemoved: Boolean
)

sealed class ChatChildEvent {
    data class Added(val message: ChatMessage) : ChatChildEvent()
    data class Changed(val message: ChatMessage) : ChatChildEvent()
    data class Removed(val message: ChatMessage) : ChatChildEvent()
}


class ChatRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository,
    private val chatRefs: ChatRefs
) {

    fun observeChatMessages(
    ): Flow<ChatChildEvent> = callbackFlow {

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(ChatMessage::class.java)?.let { trySend(ChatChildEvent.Added(it)) }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(ChatMessage::class.java)?.let { trySend(ChatChildEvent.Changed(it)) }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.getValue(ChatMessage::class.java)?.let { trySend(ChatChildEvent.Removed(it)) }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }

        chatRefs.refChat.addChildEventListener(listener)
        awaitClose { chatRefs.refChat.removeEventListener(listener) }
    }

    private val myUid = userRepository.myUid

    fun buildChatRefs(
        userId: String,
        nodeType: String
    ): ChatRefs {

        val chatId = getRefChatId(userId)

        val refAudios =
            firebaseRefsContainer.storage.reference
                .child("$NODE_CHATS/$nodeType/$chatId/")
                .child("$PATH_AUDIOS/")

        val refPhotos =
            firebaseRefsContainer.storage.reference
                .child("$NODE_CHATS/$nodeType/$chatId/")
                .child("$PATH_PHOTOS/")

        val refChat =
            firebaseRefsContainer.refChatMessageRoot
                .child(nodeType)
                .child(chatId)

        val refMyActiveChat =
            firebaseRefsContainer.refDatos
                .child(myUid)
                .child(NODE_CHATLIST)
                .child(NODE_ACTIVE_CHAT)

        val refOtherActiveChat =
            firebaseRefsContainer.refDatos
                .child(userId)
                .child(NODE_CHATLIST)
                .child(NODE_ACTIVE_CHAT)

        val refMyChatListItem =
            firebaseRefsContainer.refDatos
                .child(myUid)
                .child(nodeType)
                .child(userId)

        val refOtherChatListItem =
            firebaseRefsContainer.refDatos
                .child(userId)
                .child(nodeType)
                .child(myUid)


        return ChatRefs(
            refAudios = refAudios,
            refPhotos = refPhotos,
            refChat = refChat,
            refMyActiveChat = refMyActiveChat,
            refOtherActiveChat = refOtherActiveChat,
            refMyChatListItem = refMyChatListItem,
            refOtherChatListItem = refOtherChatListItem
        )
    }

    fun getRefChatId(
        userId: String
    ): String {
        val (first, second) = listOf(myUid, userId).sorted()
        return "${first}_${second}"
    }

    // 1) Su actual: qué chat tiene abierto el OTRO usuario
    suspend fun getActiveChat(
    ): String {
        return chatRefs.refOtherActiveChat
            .child(NODE_ACTIVE_CHAT)
            .get()
            .await()
            .getValue(String::class.java)
            ?: ""   // default
    }

    fun setActiveChat(value: String) {
        chatRefs.refMyActiveChat
            .child(NODE_ACTIVE_CHAT)
            .setValue(value)
    }

    suspend fun getChatWith(firstUid: String, secondUid: String, nodeType: String): ChatWith? {
        val snapshot = firebaseRefsContainer.refDatos
            .child(firstUid)
            .child(nodeType)
            .child(secondUid)
            .get()
            .await()

        return snapshot.getValue(ChatWith::class.java)
    }

    suspend fun saveChatWith(
        ownerUid: String,
        nodeType: String,
        otherUid: String,
        chatWith: ChatWith
    ) {
        firebaseRefsContainer.refDatos
            .child(ownerUid)
            .child(nodeType)
            .child(otherUid)
            .setValue(chatWith)
            .await()
    }

    suspend fun pushMessageToChat(
        message: ChatMessage
    ) {
        chatRefs.refChat.push().setValue(message).await()
    }

    suspend fun uploadChatData(
        uri: Uri,
        fileName: String,
        refData: StorageReference
    ): String? {
        return try {
            val fileRef = refData
                .child(fileName)
            fileRef.putFile(uri).await()
            fileRef.downloadUrl.await().toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun markChatAsSeen(
    ) {

        val dsMain = chatRefs.refMyChatListItem.get().await()
        if (!dsMain.exists()) return

        val msgDescontar = dsMain.child(MSG_RECEIVED_UNREAD).getValue(Int::class.java) ?: 0
        if (msgDescontar > 0) {
            setDoubleCheckOnLastMessages(msgDescontar)
        }

        chatRefs.refMyChatListItem.child("wVisto").setValue(MSG_SEEN)  // Mensaje
        chatRefs.refMyChatListItem.child(MSG_RECEIVED_UNREAD).setValue(0)  // Mensajes sin leer (card) = 0
    }

    private suspend fun setDoubleCheckOnLastMessages(
        count: Int
    ) {
        val snapshot = chatRefs.refChat
            .orderByChild("date")
            .limitToLast(count)
            .get()
            .await()

        if (!snapshot.exists()) return

        for (snap in snapshot.children) {
            if (snap.hasChild(MSG_SEEN_STATUS)) {
                snap.ref.child(MSG_SEEN_STATUS).setValue(3) // Tilde azul en los mensajes no leídos (count)
            }
        }
    }


    suspend fun deleteMessages(
        selected: List<ChatMessage>?,
        deleteMessages: Boolean = true
    ): DeleteResult {

        // Si es "ocultar chat" y no hay mensajes seleccionados → solo ocultar
        if (!deleteMessages && selected == null) {
            chatRefs.refMyChatListItem.child("estado").setValue(CHAT_STATE_HIDE).await()
            return DeleteResult(deletedCount = 0, chatRemoved = false)
        }

        // Si selected es null, cargar todos los mensajes del chat
        val messagesToDelete = selected ?: run {
            val snap = chatRefs.refChat.get().await()
            snap.children.mapNotNull { it.getValue(ChatMessage::class.java) }
        }

        // Borrar / marcar mensajes uno por uno
        for (chat in messagesToDelete) {
            val snap = chatRefs.refChat
                .orderByChild("date")
                .equalTo(chat.date)
                .get()
                .await()

            if (snap.exists()) {
                iterateDelete(
                    snap = snap,
                    chat = chat
                )
            }
        }

        // 2) Ver si hay que eliminar el nodo /Datos/myUid/CHATWITH/userId
        val chatRemoved = removeChatWith(
            selectedCount = messagesToDelete.size
        )

        return DeleteResult(
            deletedCount = messagesToDelete.size,
            chatRemoved = chatRemoved
        )
    }

    private suspend fun iterateDelete(
        snap: DataSnapshot,
        chat: ChatMessage
    ) {
        for (snap in snap.children) {
            val type = snap.child("type").getValue(Int::class.java) ?: continue
            val sender = snap.child("envia").getValue(String::class.java) ?: continue

            if (sender == myUid) {
                when (type) {
                    MSG_TEXT ->
                        snap.child("type").ref.setValue(MSG_TEXT_SENDER_DLT).await()

                    MSG_TEXT_RECEIVER_DLT ->
                        snap.ref.removeValue().await()

                    MSG_PHOTO ->
                        snap.child("type").ref.setValue(MSG_PHOTO_SENDER_DLT).await()

                    MSG_PHOTO_RECEIVER_DLT -> {
                        chatRefs.refPhotos.child(chat.message).delete().await()
                        snap.ref.removeValue().await()
                    }

                    MSG_AUDIO ->
                        snap.child("type").ref.setValue(MSG_AUDIO_SENDER_DLT).await()

                    MSG_AUDIO_RECEIVER_DLT -> {
                        chatRefs.refAudios.child(chat.message).delete().await()
                        snap.ref.removeValue().await()
                    }
                }
            } else {
                when (type) {
                    MSG_TEXT ->
                        snap.child("type").ref.setValue(MSG_TEXT_RECEIVER_DLT).await()

                    MSG_TEXT_SENDER_DLT ->
                        snap.ref.removeValue().await()

                    MSG_PHOTO ->
                        snap.child("type").ref.setValue(MSG_PHOTO_RECEIVER_DLT).await()

                    MSG_PHOTO_SENDER_DLT -> {
                        chatRefs.refPhotos.child(chat.message).delete().await()
                        snap.ref.removeValue().await()
                    }

                    MSG_AUDIO ->
                        snap.child("type").ref.setValue(MSG_AUDIO_RECEIVER_DLT).await()

                    MSG_AUDIO_SENDER_DLT -> {
                        chatRefs.refAudios.child(chat.message).delete().await()
                        snap.ref.removeValue().await()
                    }
                }
            }
        }
    }

    private suspend fun removeChatWith(
        selectedCount: Int
    ): Boolean {

        val data = chatRefs.refChat.get().await()

        val messages = data.childrenCount
        var senderDeleted = 0L
        var receiverDeleted = 0L

        for (snap in data.children) {
            val chat = snap.getValue(ChatMessage::class.java) ?: continue
            if (chat.sender == myUid) {
                when (chat.type) {
                    MSG_TEXT_SENDER_DLT,
                    MSG_PHOTO_SENDER_DLT,
                    MSG_AUDIO_SENDER_DLT -> senderDeleted++
                }
            } else {
                when (chat.type) {
                    MSG_TEXT_RECEIVER_DLT,
                    MSG_PHOTO_RECEIVER_DLT,
                    MSG_AUDIO_RECEIVER_DLT -> receiverDeleted++
                }
            }
        }

        val count = messages - (senderDeleted + receiverDeleted + selectedCount)
        if (count == 0L) {

            chatRefs.refMyChatListItem
                .removeValue()
                .await()

            return true
        }
        return false
    }

    suspend fun getMessageCount(): Int {
        return chatRefs.refChat.get().await().childrenCount.toInt()
    }

}
