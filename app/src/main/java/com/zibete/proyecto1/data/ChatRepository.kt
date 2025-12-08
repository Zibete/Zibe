package com.zibete.proyecto1.data

import android.net.Uri
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatWith
import kotlin.collections.sorted
import kotlin.collections.listOf
import com.zibete.proyecto1.ui.constants.Constants.MSG_RECEIVED_UNREAD
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN_STATUS
import com.zibete.proyecto1.ui.constants.Constants.NODE_ACTIVE_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_MESSAGES
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatRefs(
    val refMyReceiverData: StorageReference,
    val refOtherReceiverData: StorageReference,
    val refMyActiveChat: DatabaseReference,
    val refOtherActiveChat: DatabaseReference,
    val refChatId: DatabaseReference,
)

sealed class ChatChildEvent {
    data class Added(val snapshot: DataSnapshot) : ChatChildEvent()
    data class Changed(val snapshot: DataSnapshot) : ChatChildEvent()
    data class Removed(val snapshot: DataSnapshot) : ChatChildEvent()
}

class ChatRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {

    private val myUid = userRepository.myUid

    fun buildChatRefs(
        userId: String,
        nodeType: String
    ): ChatRefs {
        val storageRoot = firebaseRefsContainer.storage.reference

        val refMyReceiverData =
            storageRoot.child("$nodeType/$myUid/")

        val refOtherReceiverData =
            storageRoot.child("$nodeType/$userId/")

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

        val (first, second) = listOf(myUid, userId).sorted()
        val refChatId =
            firebaseRefsContainer.refChatMessageRoot
                .child(nodeType)
                .child("${first}_${second}")
                .child(NODE_MESSAGES)

        return ChatRefs(
            refMyReceiverData = refMyReceiverData,
            refOtherReceiverData = refOtherReceiverData,
            refMyActiveChat = refMyActiveChat,
            refOtherActiveChat = refOtherActiveChat,
            refChatId= refChatId
        )
    }

    // User de grupo deja de estar disponible
    fun observeGroupUserAvailability(
        groupName: String,
        userId: String
    ): Flow<Boolean> = callbackFlow {

        val ref = firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(ds: DataSnapshot) {

                val isAvailable = ds.exists()

                trySend(isAvailable)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeChatMessages(
        refChatId: DatabaseReference
    ): Flow<ChatChildEvent> = callbackFlow {

        val listener = object : ChildEventListener {
            override fun onChildAdded(ds: DataSnapshot, prev: String?) {
                trySend(ChatChildEvent.Added(ds))
            }

            override fun onChildChanged(ds: DataSnapshot, prev: String?) {
                trySend(ChatChildEvent.Changed(ds))
            }

            override fun onChildRemoved(ds: DataSnapshot) {
                trySend(ChatChildEvent.Removed(ds))
            }

            override fun onChildMoved(ds: DataSnapshot, prev: String?) {}
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        refChatId.addChildEventListener(listener)
        awaitClose { refChatId.removeEventListener(listener) }
    }

    // 1) Su actual: qué chat tiene abierto el OTRO usuario
    suspend fun getActiveChat(userId: String, chatType: String): String {
        val chatRef = firebaseRefsContainer.refDatos
            .child(userId)
            .child(NODE_CHATLIST)

        return chatRef
            .child(NODE_ACTIVE_CHAT)
            .get()
            .await()
            .getValue(String::class.java)
            ?: ""   // default
    }

    fun setActiveChat(userId: String, value: String) {
        firebaseRefsContainer.refDatos
            .child(userId)
            .child(NODE_CHATLIST)
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
        chatRef: DatabaseReference,
        message: ChatMessage
    ) {
        chatRef.push().setValue(message).await()
    }

    suspend fun uploadChatData(
        uri: Uri,
        fileName: String,
        path: String,
        refOtherReceiverData: StorageReference): String? {
        return try {
            val ref = refOtherReceiverData
                .child(myUid)
                .child(path)
                .child(fileName)

            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) {
            null
        }
    }



    suspend fun markChatAsSeen(
        userId: String,
        nodeType: String,          // NODE_CURRENT_CHAT o NODE_GROUP_CHAT
        refChatId: DatabaseReference
    ) {

        val chatMetaRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(nodeType)
            .child(userId)

        val dsMain = chatMetaRef.get().await()
        if (!dsMain.exists()) return

        val msgDescontar = dsMain.child(MSG_RECEIVED_UNREAD).getValue(Int::class.java) ?: 0
        if (msgDescontar > 0) {
            setDoubleCheckOnLastMessages(refChatId, msgDescontar)
        }

        // Actualizar flags de meta
        chatMetaRef.child("wVisto").setValue(MSG_SEEN)  // Mensaje
        chatMetaRef.child(MSG_RECEIVED_UNREAD).setValue(0)  // Mensajes sin leer (card) = 0
    }

    private suspend fun setDoubleCheckOnLastMessages(
        refChatId: DatabaseReference,
        count: Int
    ) {
        val snapshot = refChatId
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




}
