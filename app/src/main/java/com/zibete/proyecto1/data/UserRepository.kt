package com.zibete.proyecto1.data

import android.content.Context
import android.location.Location
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.R
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.State
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.EMPTY
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_ChatMessage
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_MESSAGES
import com.zibete.proyecto1.utils.Utils.today
import com.zibete.proyecto1.utils.Utils.now
import com.zibete.proyecto1.utils.Utils.time
import com.zibete.proyecto1.utils.Utils.yesterday
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


// Clase que maneja la comunicación con Firebase para datos de usuario
// (repository = repositorio → capa de acceso a datos)
@Singleton
class UserRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userSessionManager: UserSessionManager,
    @ApplicationContext private val context: Context
) {

    val myUid get() = userSessionManager.myUid
    val user get() = userSessionManager.user
    val latitude: Double get() = userSessionManager.latitude
    val longitude: Double get() = userSessionManager.longitude


    fun updateMyLocation(lat: Double, lon: Double) {
        userSessionManager.latitude = lat
        userSessionManager.longitude = lon
    }

// UserRepository

    suspend fun getChatPhotosWithUser(otherUid: String): List<String> {

        val pathA = firebaseRefsContainer.refChat
            .child("$myUid <---> $otherUid")
            .child(NODE_MESSAGES)

        val pathB = firebaseRefsContainer.refChat
            .child("$otherUid <---> $myUid")
            .child(NODE_MESSAGES)

        val photos = mutableListOf<String>()

        fun collectFrom(snapshot: DataSnapshot) {
            for (msgSnap in snapshot.children) {
                val type = msgSnap.child("type").getValue(Int::class.java)
                val sender = msgSnap.child("envia").getValue(String::class.java)
                val message = msgSnap.child("mensaje").getValue(String::class.java)

                if (sender != null && sender != myUid) {
                    if (type == Constants.MSG_PHOTO || type == Constants.MSG_PHOTO_SENDER_DLT) {
                        if (!message.isNullOrEmpty()) {
                            photos.add(message)
                        }
                    }
                }
            }
        }

        val snapA = pathA.get().await()
        if (snapA.exists()) collectFrom(snapA)

        val snapB = pathB.get().await()
        if (snapB.exists()) collectFrom(snapB)

        // si querés evitar duplicados:
        return photos.distinct()
    }

    suspend fun isUserFavorite(otherUid: String): Boolean {
        val snap = firebaseRefsContainer.refDatos
            .child(myUid)
            .child("FavoriteList")
            .child(otherUid)
            .get()
            .await()

        return snap.exists()
    }

    suspend fun addFavoriteUser(otherUid: String) {
        firebaseRefsContainer.refDatos
            .child(myUid)
            .child("FavoriteList")
            .child(otherUid)
            .setValue(otherUid)
            .await()
    }

    suspend fun removeFavoriteUser(otherUid: String) {
        firebaseRefsContainer.refDatos
            .child(myUid)
            .child("FavoriteList")
            .child(otherUid)
            .removeValue()
            .await()
    }

    data class BlockState(
        val iBlockedUser: Boolean,
        val userBlockedMe: Boolean
    )
    suspend fun getBlockStateWith(otherUid: String): BlockState {
        val meSnap = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(NODE_CURRENT_CHAT)
            .child(otherUid)
            .child("estado")
            .get()
            .await()

        val otherSnap = firebaseRefsContainer.refDatos
            .child(otherUid)
            .child(NODE_CURRENT_CHAT)
            .child(myUid)
            .child("estado")
            .get()
            .await()

        val iBlocked = meSnap.getValue(String::class.java) == CHAT_STATE_BLOQ
        val blockedMe = otherSnap.getValue(String::class.java) == CHAT_STATE_BLOQ

        return BlockState(
            iBlockedUser = iBlocked,
            userBlockedMe = blockedMe
        )
    }

    fun observeUnreadChats(): Flow<Int> = callbackFlow {
        val query = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(NODE_CURRENT_CHAT)
            .orderByChild("noVisto")
            .startAt(1.0)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (child in snapshot.children) {
                    count += child.child("noVisto").getValue(Int::class.java) ?: 0
                }
                trySend(count)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun getUserProfile(id: String): Users? =
        firebaseRefsContainer.refCuentas
            .child(id)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(Users::class.java)





    suspend fun markMessagesAsSeen(otherUserId: String, chatType: String, noSeen: Int ) {
        if (noSeen <= 0) return

        val path1 = "$myUid <---> $otherUserId"
        val path2 = "$otherUserId <---> $myUid"

        fun markSeen(ds: DataSnapshot) {
            for (msg in ds.children) {
                msg.ref.child("visto").setValue(2)
            }
        }

        // 1) Intentar con userA <---> userB
        val snap1 = firebaseRefsContainer.refChat
            .child(path1)
            .child(NODE_MESSAGES)
            .limitToLast(noSeen)
            .get()
            .await()

        if (snap1.exists()) {
            markSeen(snap1)
        } else {
            // 2) Intentar con userB <---> userA
            val snap2 = firebaseRefsContainer.refChat
                .child(path2)
                .child(NODE_MESSAGES)
                .limitToLast(noSeen)
                .get()
                .await()

            if (snap2.exists()) {
                markSeen(snap2)
            }
        }

        // 3) Actualizar los contadores como siempre
        markAsReadChatList(otherUserId, chatType)
    }

    suspend fun markAsReadChatList(userId: String, chatType: String) {
        val chatWithRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)        // ← NODE_CURRENT_CHAT o "CHATWITHUNKNOWN"
            .child(userId)      // ← el ID del otro usuario
        val noSeenRef = chatWithRef
            .child("noVisto")
        val noSeenChatList = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(NODE_CHATLIST)
            .child("msgNoLeidos")

        // Leer ambos valores en paralelo
        val noSeenSnap = noSeenRef.get().await()
        val noSeenListSnap = noSeenChatList.get().await()

        val currentNoSeenChatWith = noSeenSnap.getValue(Int::class.java) ?: 0
        val currentNoSeenChatList = noSeenListSnap.getValue(Int::class.java) ?: 0

        // Toggle: si tenía mensajes no leídos → marcar como leído (0), sino → +1
        if (currentNoSeenChatWith > 0) {
            noSeenRef.setValue(0).await()
            noSeenChatList.setValue(currentNoSeenChatList - currentNoSeenChatWith).await()
        } else {
            noSeenRef.setValue(1).await()
            noSeenChatList.setValue(currentNoSeenChatList + 1).await()
        }
    }

    suspend fun getChatStateWith(chatWithId: String, chatType: String): String {
        val chatRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)
            .child(chatWithId)

        return chatRef
            .child("estado")
            .get()
            .await()
            .getValue(String::class.java)
            ?: NODE_CURRENT_CHAT   // default
    }

    suspend fun updateStateChatWith(
        userId: String,
        userName: String,
        nodeType: String,
        newState: String
    ) {
        val chatRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(nodeType)
            .child(userId)

        val snapshot = chatRef.get().await()

        // Si no existe el nodo, lo creamos con default
        if (!snapshot.exists()) {
            val newChat = createDefaultChatWith(userId, userName)
            newChat.state = newState          // estado inicial definido
            chatRef.setValue(newChat).await()
            return
        }

        val photo = snapshot.child("wUserPhoto").getValue(String::class.java).orEmpty()

        // Caso historico: si la foto está vacía → limpiar nodo zombie
        if (photo == EMPTY) {
            chatRef.removeValue().await()
            return
        }

        // Aplicar estado nuevo
        chatRef.child("estado").setValue(newState).await()
    }


    suspend fun deleteChat(idUser: String, userName: String, nodeType: String, deleteMessages: Boolean) {
        val ref = if (nodeType == NODE_CURRENT_CHAT) Constants.NODE_ChatMessage else Constants.NODE_GROUP_CHAT
        val startedByMe = firebaseRefsContainer.refChatMessageRoot.child(ref).child("$myUid <---> $idUser").child(NODE_MESSAGES)
        val startedByHim = firebaseRefsContainer.refChatMessageRoot.child(ref).child("$idUser <---> $myUid").child(NODE_MESSAGES) // No va más

        val messagesSnap = startedByMe.get().await()
        val messages = messagesSnap.children.mapNotNull { it.getValue(ChatMessage::class.java) }

        if (messages.isEmpty()) return

        if (deleteMessages) {
            messages.forEach { message ->
                val isMine = message.sender == myUid
                val newType = when {
                    isMine && message.type == Constants.MSG_TEXT -> Constants.MSG_TEXT_SENDER_DLT
                    !isMine && message.type == Constants.MSG_TEXT -> Constants.MSG_TEXT_RECEIVER_DLT
                    isMine && message.type == Constants.MSG_PHOTO -> Constants.MSG_PHOTO_SENDER_DLT
                    !isMine && message.type == Constants.MSG_PHOTO -> Constants.MSG_PHOTO_RECEIVER_DLT
                    isMine && message.type == Constants.MSG_AUDIO -> Constants.MSG_AUDIO_SENDER_DLT
                    !isMine && message.type == Constants.MSG_AUDIO -> Constants.MSG_AUDIO_RECEIVER_DLT
                    else -> return@forEach
                }

                val messageRef = startedByMe.child(message.date)
                messageRef.child("type").setValue(newType).await()

                if (!isMine && newType in listOf(Constants.MSG_PHOTO_SENDER_DLT, Constants.MSG_AUDIO_SENDER_DLT)) {
                    deleteRemoteFile(firebaseRefsContainer.storage.reference.child("$nodeType/$myUid/"), message)
                    messageRef.removeValue().await()
                } else if (isMine && newType in listOf(Constants.MSG_PHOTO_RECEIVER_DLT, Constants.MSG_AUDIO_RECEIVER_DLT)) {
                    deleteRemoteFile(firebaseRefsContainer.storage.reference.child("$nodeType/$idUser/"), message)
                    messageRef.removeValue().await()
                }
            }
            firebaseRefsContainer.refDatos.child(myUid).child(nodeType).child(idUser).removeValue().await()
        } else {
            firebaseRefsContainer.refDatos.child(myUid).child(nodeType).child(idUser).child("estado").setValue(CHAT_STATE_HIDE).await()
        }
    }
    suspend fun getMessageCount(idUser: String, nodeType: String): Int {
        var count = 0

        val ref = if (nodeType == NODE_CURRENT_CHAT) NODE_ChatMessage else NODE_GROUP_CHAT
        val startedByMe = firebaseRefsContainer.refChatMessageRoot.child(ref).child("$myUid <---> $idUser").child(NODE_MESSAGES)

        val messagesSnap = startedByMe.get().await()

        for (snap in messagesSnap.children) {
            val chat = snap.getValue(ChatMessage::class.java) ?: continue
            val isMine = chat.sender == myUid

            val validTypesMine = listOf(
                Constants.MSG_TEXT,
                Constants.MSG_PHOTO,
                Constants.MSG_AUDIO,
                Constants.MSG_TEXT_RECEIVER_DLT,
                Constants.MSG_PHOTO_RECEIVER_DLT,
                Constants.MSG_AUDIO_RECEIVER_DLT
            )

            val validTypesOther = listOf(
                Constants.MSG_TEXT,
                Constants.MSG_PHOTO,
                Constants.MSG_AUDIO,
                Constants.MSG_TEXT_SENDER_DLT,
                Constants.MSG_PHOTO_SENDER_DLT,
                Constants.MSG_AUDIO_SENDER_DLT
            )

            if (isMine && chat.type in validTypesMine) count++
            if (!isMine && chat.type in validTypesOther) count++
        }

        return count
    }
    private suspend fun deleteRemoteFile(ref: StorageReference, chat: ChatMessage) {
        val msg = chat.message
        val start = msg.indexOf(myUid) + myUid.length + 3
        val ext = if (msg.contains(".jpg")) ".jpg" else ".mp3"
        val end = msg.indexOf(ext) + ext.length
        if (start in 0..end && end <= msg.length) {
            val fileName = msg.substring(start, end)
            ref.child(fileName).delete().await()
        }
    }




    private fun createDefaultChatWith(chatWithId: String, userName: String): ChatWith {
        return ChatWith(
            "",
            now(),
            null,
            "",
            chatWithId,
            userName,
            Constants.EMPTY,
            CHAT_STATE_SILENT,
            0,
            1
        )
    }

    suspend fun createUserNode(user: FirebaseUser, token: String?) = withContext(Dispatchers.IO) {

        val newUser = Users(
            user.uid,
            user.displayName.orEmpty(),
            "",
            now(),
            0,
            user.email.orEmpty(),
            user.photoUrl?.toString().orEmpty(),
            true,
            token.orEmpty(),
            0.0,
            "",
            0.0,
            0.0
        )

        firebaseRefsContainer
            .refCuentas
            .child(user.uid)
            .setValue(newUser)
            .await()
    }

    suspend fun updateLocation(location: Location) {
        firebaseRefsContainer.refCuentas
            .child(myUid)
            .updateChildren(
                mapOf(
                    "latitud" to location.latitude,
                    "longitud" to location.longitude
                )
            ).await()
    }

    suspend fun setUserRecording() {
        val state = State(context.getString(R.string.recording), "", "")
        firebaseRefsContainer.refDatos
            .child(myUid)
            .child("Estado")
            .setValue(state)
            .await()
        firebaseRefsContainer.refCuentas
            .child(myUid)
            .child("estado")
            .setValue(true)
            .await()
    }

    suspend fun setUserOnline() = withContext(Dispatchers.IO) {
        val state = State(context.getString(R.string.online), "", "")

        firebaseRefsContainer.refDatos
            .child(myUid)
            .child("Estado")
            .setValue(state)
            .await()
        firebaseRefsContainer.refCuentas
            .child(myUid)
            .child("estado")
            .setValue(true)
            .await()
    }

    suspend fun setUserOffline() = withContext(Dispatchers.IO) {
        val state = State(context.getString(R.string.offline), "", "")

        firebaseRefsContainer.refDatos
            .child(myUid)
            .child("Estado")
            .setValue(state)
            .await()
        firebaseRefsContainer.refCuentas
            .child(myUid)
            .child("estado")
            .setValue(false) // <---------------FALSE?
            .await()
    }

    suspend fun setUserLastSeen() = withContext(Dispatchers.IO) {

        val state = State(
            context.getString(R.string.ultVez),
            today(),
            time())

        firebaseRefsContainer.refDatos
            .child(myUid)
            .child("Estado")
            .setValue(state)
            .await()
        firebaseRefsContainer.refCuentas
            .child(myUid)
            .child("estado")
            .setValue(false)
            .await()
    }

    // Convierte un DataSnapshot de Firebase en un UserStatus
    private fun DataSnapshot.toUserStatus(myUid: String,chatType: String? = null): UserStatus {
        // No existe nodo "Estado"
        if (!exists()) return UserStatus.Offline

        val estado = child("estado").getValue(String::class.java)
        val fecha = child("fecha").getValue(String::class.java)
        val hora = child("hora").getValue(String::class.java)

        // Online simple
        if (estado == context.getString(R.string.online)) return UserStatus.Online

        // Escribiendo / Grabando (y solo si es conmigo)
        if (estado == context.getString(R.string.escribiendo) ||
            estado == context.getString(R.string.recording)) {

            val currentChat = child(key!!).child("ChatList/Actual").getValue(String::class.java)

            if (currentChat == myUid + chatType) {
                return UserStatus.TypingOrRecording(estado)
            }

            return UserStatus.Online // está escribiendo pero no conmigo → muestro online
        }

        // Última vez
        val lastSeenText = when (fecha) {
            today() -> "Hoy a las $hora"
            yesterday() -> "Ayer a las $hora"
            else -> "$fecha a las $hora"
        }
        return UserStatus.LastSeen("Últ. vez $lastSeenText")
    }

    // Observa los cambios en el estado de un usuario y emite UserStatus
    fun observeUserStatus(userId: String,chatType: String? = null): Flow<UserStatus> = callbackFlow {
        val listener = firebaseRefsContainer.refDatos
            .child(userId)
            .child("Estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.toUserStatus(myUid, chatType))
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            })

        awaitClose {
            firebaseRefsContainer.refDatos.child(userId).child("Estado")
                .removeEventListener(listener)
        }
    }.flowOn(Dispatchers.IO)

}