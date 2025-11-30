package com.zibete.proyecto1.data

import android.content.Context
import android.location.Location
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.R
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.model.State
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.Utils.now
import com.zibete.proyecto1.utils.Utils.today
import com.zibete.proyecto1.utils.Utils.yesterday
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

    private val myUid = userSessionManager.uid

    suspend fun markAsReadChat(chatWithId: String, chatType: String) {
        val chatNode = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)        // ← "CHATWITH" o "CHATWITHUNKNOWN"
            .child(chatWithId)      // ← el ID del otro usuario
        val noVistoRef = chatNode.child("noVisto")
        val totalNoLeidosRef = firebaseRefsContainer.refDatos.child(myUid).child("ChatList").child("msgNoLeidos")

        // Leer ambos valores en paralelo
        val noVistoSnap = noVistoRef.get().await()
        val totalNoLeidosSnap = totalNoLeidosRef.get().await()

        val currentNoVisto = noVistoSnap.getValue(Int::class.java) ?: 0
        val currentTotal = totalNoLeidosSnap.getValue(Int::class.java) ?: 0

        // Toggle: si tenía mensajes no leídos → marcar como leído (0), sino → +1
        if (currentNoVisto > 0) {
            noVistoRef.setValue(0).await()
            totalNoLeidosRef.setValue(currentTotal - currentNoVisto).await()
        } else {
            noVistoRef.setValue(1).await()
            totalNoLeidosRef.setValue(currentTotal + 1).await()
        }
    }

    suspend fun getNotificationState(chatWithId: String, chatType: String): String {
        val chatRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)
            .child(chatWithId)

        return chatRef
            .child("estado")
            .get()
            .await()
            .getValue(String::class.java)
            ?: chatType   // default lógico: notificaciones activadas
    }

    suspend fun updateNotificationState(chatWithId: String, chatType: String, userName: String) {
        val chatRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)
            .child(chatWithId)

        val snapshot = chatRef.get().await()

        if (!snapshot.exists()) {
            // Crear ChatWith nuevo en estado "silent"
            val newChat = createDefaultChatWith(chatWithId, userName)
            chatRef.setValue(newChat).await()
            return
        }

        val currentState = snapshot.child("estado").getValue(String::class.java)
        val photo = snapshot.child("wUserPhoto").getValue(String::class.java).orEmpty()

        // Si la foto está vacía → eliminar nodo: cuando un usuario borraba su cuenta o se eliminaba del chat,
        // no se borraba el nodo completo en /Datos/otroUid/CHATWITH/miUid cod 2020
        if (photo == Constants.EMPTY) {
            chatRef.removeValue().await()
            return
        }

        // Toggle: "silent" ↔ chatType (ej: "CHATWITH")
        val newState = if (currentState == "silent") chatType else "silent"
        chatRef.child("estado").setValue(newState).await()
    }

    suspend fun blockUser(chatWithId: String, chatType: String, userName: String) {

        val chatRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)
            .child(chatWithId)

        val snapshot = chatRef.get().await()

        if (!snapshot.exists()) {
            val newChatWith = createDefaultChatWith(chatWithId, userName)
            chatRef.setValue(newChatWith).await()
        }

        chatRef.child("estado").setValue("bloq").await()
    }

    suspend fun unblockUser(chatWithId: String, chatType: String) {
        val chatRef = firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)
            .child(chatWithId)

        val snapshot = chatRef.get().await()
        if (!snapshot.exists()) return

        val photo = snapshot.child("wUserPhoto").getValue(String::class.java).orEmpty()

        // BORRAR -->
        // Si la foto está vacía → eliminar nodo: cuando un usuario borraba su cuenta o se eliminaba del chat,
        // no se borraba el nodo completo en /Datos/otroUid/CHATWITH/miUid cod 2020
        if (photo == Constants.EMPTY) {
            chatRef.removeValue().await()
        } else {
            chatRef.child("estado").setValue(chatType).await()
        }
    }

    suspend fun hideChat(chatWithId: String, chatType: String) {
        firebaseRefsContainer.refDatos
            .child(myUid)
            .child(chatType)
            .child(chatWithId)
            .child("estado")
            .setValue("delete")
            .await()
    }

    suspend fun deleteChat(chatWithId: String, chatType: String, deleteMessages: Boolean) {
        val ref = if (chatType == Constants.CHATWITH) Constants.CHAT else Constants.UNKNOWN
        val startedByMe = firebaseRefsContainer.refChatsRoot.child(ref).child("$myUid <---> $chatWithId").child("Mensajes")
        val startedByHim = firebaseRefsContainer.refChatsRoot.child(ref).child("$chatWithId <---> $myUid").child("Mensajes")

        val messagesSnap = startedByMe.get().await()
        val messages = messagesSnap.children.mapNotNull { it.getValue(Chats::class.java) }

        if (messages.isEmpty()) return

        if (deleteMessages) {
            messages.forEach { message ->
                val isMine = message.sender == myUid
                val newType = when {
                    isMine && message.type == Constants.MSG -> Constants.MSG_SENDER_DLT
                    !isMine && message.type == Constants.MSG -> Constants.MSG_RECEIVER_DLT
                    isMine && message.type == Constants.PHOTO -> Constants.PHOTO_SENDER_DLT
                    !isMine && message.type == Constants.PHOTO -> Constants.PHOTO_RECEIVER_DLT
                    isMine && message.type == Constants.AUDIO -> Constants.AUDIO_SENDER_DLT
                    !isMine && message.type == Constants.AUDIO -> Constants.AUDIO_RECEIVER_DLT
                    else -> return@forEach
                }

                val messageRef = startedByMe.child(message.date)
                messageRef.child("type").setValue(newType).await()

                if (!isMine && newType in listOf(Constants.PHOTO_SENDER_DLT, Constants.AUDIO_SENDER_DLT)) {
                    deleteRemoteFile(Constants.storageReference.child("$chatType/$myUid/"), message)
                    messageRef.removeValue().await()
                } else if (isMine && newType in listOf(Constants.PHOTO_RECEIVER_DLT, Constants.AUDIO_RECEIVER_DLT)) {
                    deleteRemoteFile(Constants.storageReference.child("$chatType/$chatWithId/"), message)
                    messageRef.removeValue().await()
                }
            }
            firebaseRefsContainer.refDatos.child(myUid).child(chatType).child(chatWithId).removeValue().await()
        } else {
            firebaseRefsContainer.refDatos.child(myUid).child(chatType).child(chatWithId).child("estado").setValue("delete").await()
        }
    }

    private suspend fun deleteRemoteFile(ref: StorageReference, chat: Chats) {
        val msg = chat.message
        val start = msg.indexOf(myUid) + myUid.length + 3
        val ext = if (msg.contains(".jpg")) ".jpg" else ".mp3"
        val end = msg.indexOf(ext) + ext.length
        if (start in 0..end && end <= msg.length) {
            val fileName = msg.substring(start, end)
            ref.child(fileName).delete().await()
        }
    }







    fun setUnBlockUser(
        context: Context,
        idUser: String?,
        nameUser: String?,
        view: View,
        type: String?
    ) {
        if (idUser == null || nameUser == null || type == null) return

        UserMessageUtils.confirm(
            context = context,
            title = "Desbloquear",
            message = "¿Desea desbloquear a $nameUser?",
            onConfirm = {
                firebaseRefsContainer.refDatos.child(myUid).child(type).child(idUser)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val photo = dataSnapshot.child("wUserPhoto").getValue(String::class.java)
                            if (photo == Constants.EMPTY) {
                                dataSnapshot.ref.removeValue()
                            } else {
                                dataSnapshot.ref.child("estado").setValue(type)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

                UserMessageUtils.showSnack(
                    root = view,
                    message = "Desbloqueaste a $nameUser",
                    duration = Snackbar.LENGTH_SHORT,
                    iconRes = R.drawable.ic_info_24
                )
            }
        )
    }

//    fun bindBlockStatus(
//        user_id: String,
//        profile_bloc: ImageView
//    ) { // bindBlockStatus = vincular estado de bloqueo
//
//        firebaseRefsContainer.refDatos.child(myUid).child(Constants.CHATWITH).child(user_id).child("estado")
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    if (dataSnapshot.getValue<String?>(String::class.java) == "bloq") {
//                        profile_bloc.visibility = View.VISIBLE
//                    } else {
//                        profile_bloc.visibility = View.GONE
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                }
//            })
//    }


    private fun createDefaultChatWith(
        chatWithId: String,
        userName: String
    ): ChatWith {
        return ChatWith(
            "",
            now(),
            null,
            "",
            chatWithId,
            userName,
            Constants.EMPTY,
            "silent",
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


    suspend fun setUserOnline() = withContext(Dispatchers.IO) {
        val state = State(context.getString(R.string.online), "", "")

        firebaseRefsContainer.refDatos.child(myUid).child("Estado").setValue(state).await()
        firebaseRefsContainer.refCuentas.child(myUid).child("estado").setValue(true).await()
    }

    suspend fun setUserOffline() = withContext(Dispatchers.IO) {
        val state = State(context.getString(R.string.offline), "", "")

        firebaseRefsContainer.refDatos.child(myUid).child("Estado").setValue(state).await()
        firebaseRefsContainer.refCuentas.child(myUid).child("estado").setValue(true).await()
    }

    suspend fun setUserLastSeen() = withContext(Dispatchers.IO) {
        val c = Calendar.getInstance()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(c.time)
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)

        val state = State(context.getString(R.string.ultVez), date, time)

        firebaseRefsContainer.refDatos.child(myUid).child("Estado").setValue(state).await()
        firebaseRefsContainer.refCuentas.child(myUid).child("estado").setValue(false).await()
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
            estado == context.getString(R.string.grabando)) {

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