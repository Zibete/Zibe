package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.constants.Constants.NODE_ACTIVE_CHAT_UID
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

    val refStartedByMe: DatabaseReference,
    val refStartedByOther: DatabaseReference
)

class ChatRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {

    private val myUid = userRepository.myUid

    suspend fun getUserToken(userId: String): String? {
        val snap = firebaseRefsContainer.refCuentas
            .child(userId)
            .child("token")
            .get()
            .await()

        return snap.getValue(String::class.java)
    }

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
                .child(NODE_ACTIVE_CHAT_UID)

        val refOtherActiveChat =
            firebaseRefsContainer.refDatos
                .child(userId)
                .child(NODE_CHATLIST)
                .child(NODE_ACTIVE_CHAT_UID)

        val refStartedByMe =
            firebaseRefsContainer.refChatsRoot
                .child(nodeType)
                .child("$myUid <---> $userId")
                .child(NODE_MESSAGES)

        val refStartedByOther =
            firebaseRefsContainer.refChatsRoot
                .child(nodeType)
                .child("$userId <---> $myUid")
                .child(NODE_MESSAGES)

        return ChatRefs(
            refMyReceiverData = refMyReceiverData,
            refOtherReceiverData = refOtherReceiverData,
            refMyActiveChat = refMyActiveChat,
            refOtherActiveChat = refOtherActiveChat,
            refStartedByMe = refStartedByMe,
            refStartedByOther = refStartedByOther
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

}
