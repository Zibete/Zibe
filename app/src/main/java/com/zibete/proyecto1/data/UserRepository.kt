package com.zibete.proyecto1.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.location.Location
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.State
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.UserMessageUtils
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

    suspend fun setUserOnline() = withContext(Dispatchers.IO) {
        val state = State(context.getString(R.string.online), "", "")

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


    // ------------------------------------------------------------
    // Convierte un DataSnapshot de Firebase en un UserStatus
    private fun DataSnapshot.toUserStatus(
        myUid: String,
        chatType: String? = null
    ): UserStatus {
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
    fun observeUserStatus(
        userId: String,
        chatType: String? = null
    ): Flow<UserStatus> = callbackFlow {
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



    fun silent(nameUser: String?, idUser: String?, type: String?) {
        firebaseRefsContainer.refDatos.child(myUid).child(type!!).child(idUser!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val state =
                            dataSnapshot.child("estado").getValue(String::class.java)
                        val photo =
                            dataSnapshot.child("wUserPhoto").getValue(String::class.java)

                        if (photo == Constants.EMPTY) {
                            dataSnapshot.ref.removeValue()
                        } else {
                            if (state == "silent") {
                                dataSnapshot.ref.child("estado").setValue(type)
                            } else {
                                dataSnapshot.ref.child("estado").setValue("silent")
                            }
                        }
                    } else {
                        newChatWith(dataSnapshot, idUser, nameUser!!, "silent")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun newChatWith(dataSnapshot: DataSnapshot, idUser: String, nameUser: String, state: String) {
        val c = Calendar.getInstance()
        val dateFormat3 = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")
        val newChat = ChatWith(
            "",
            dateFormat3.format(c.getTime()),
            null,
            "",
            idUser,
            nameUser,
            Constants.EMPTY,
            state,
            0,
            1
        )

        dataSnapshot.ref.setValue(newChat)
    }


    fun setBlockUser(
        context: Context,
        nameUser: String?,
        idUser: String?,
        view: View,
        type: String?
    ) {
        if (nameUser == null || idUser == null || type == null) return

        UserMessageUtils.confirm(
            context = context,
            title = "Bloquear",
            message = "¿Desea bloquear a $nameUser?",
            onConfirm = {
                firebaseRefsContainer.refDatos.child(myUid).child(type).child(idUser)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                dataSnapshot.ref.child("estado").setValue("bloq")
                            } else {
                                newChatWith(dataSnapshot, idUser, nameUser, "bloq")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

                UserMessageUtils.showSnack(
                    root = view,
                    message = "Bloqueaste a $nameUser, podrás desbloquearlo cuando desees",
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = "OK",
                    action = {},
                    iconRes = R.drawable.ic_info_24
                )
            }
        )
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

    fun bindBlockStatus(
        user_id: String,
        profile_bloc: ImageView
    ) { // bindBlockStatus = vincular estado de bloqueo

        firebaseRefsContainer.refDatos.child(myUid).child(Constants.CHATWITH).child(user_id).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.getValue<String?>(String::class.java) == "bloq") {
                        profile_bloc.visibility = View.VISIBLE
                    } else {
                        profile_bloc.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }


    var latitude: Double = 0.0

    var longitude: Double = 0.0

    fun updateLocationUI(mLastLocation: Location?) {
        if (mLastLocation == null) return

        latitude = mLastLocation.latitude
        longitude = mLastLocation.longitude

        firebaseRefsContainer.refCuentas.child(myUid).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    firebaseRefsContainer.refCuentas.child(myUid).child("latitud").setValue(latitude)
                    firebaseRefsContainer.refCuentas.child(myUid).child("longitud").setValue(
                        longitude
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    companion object
}