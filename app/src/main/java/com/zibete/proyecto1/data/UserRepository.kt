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
    private val sessionManager: UserSessionManager,
    @ApplicationContext private val context: Context
) {

    suspend fun setUserOnline() = withContext(Dispatchers.IO) {
        val state = State(context.getString(R.string.online), "", "")

        firebaseRefsContainer.refDatos.child(sessionManager.uid).child("Estado").setValue(state).await()
        firebaseRefsContainer.refCuentas.child(sessionManager.uid).child("estado").setValue(true).await()
    }

    suspend fun setUserOffline() = withContext(Dispatchers.IO) {
        val c = Calendar.getInstance()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(c.time)
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)

        val state = State(context.getString(R.string.ultVez), date, time)

        firebaseRefsContainer.refDatos.child(sessionManager.uid).child("Estado").setValue(state).await()
        firebaseRefsContainer.refCuentas.child(sessionManager.uid).child("estado").setValue(false).await()
    }


    // ------------------------------------------------------------


    private fun DataSnapshot.toUserStatus(
        myUid: String,
        chatType: String? = null
    ): UserStatus {
        if (!exists()) return UserStatus.Offline

        val estado = child("estado").getValue(String::class.java)
        val fecha = child("fecha").getValue(String::class.java)
        val hora = child("hora").getValue(String::class.java)

        // Online simple
        if (estado == "conectado") return UserStatus.Online

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


    fun observeUserStatus(
        userId: String,
        chatType: String? = null
    ): Flow<UserStatus> = callbackFlow {
        val listener = firebaseRefsContainer.refDatos
            .child(userId)
            .child("Estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.toUserStatus(sessionManager.uid, chatType))
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



    fun stateUser(
        context: Context,
        userId: String,  // id_user → usuario a mostrar
        iconConnected: ImageView,  // icon_conectado
        iconDisconnected: ImageView,  // icon_desconectado
        tvStatus: TextView,  // tv_estado
        type: String? // type → ej. "chatWith"
    ) {
        firebaseRefsContainer.refDatos.child(userId).child("Estado").addValueEventListener(object :
            ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val c = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy")

                    val estado = dataSnapshot.child("estado")
                        .getValue(String::class.java) // estado → "conectado", "escribiendo", etc.
                    val fecha = dataSnapshot.child("fecha")
                        .getValue(String::class.java) // fecha última vez
                    val hora = dataSnapshot.child("hora")
                        .getValue(String::class.java) // hora última vez

                    if (estado != null && estado == context.getString(R.string.online)) {
                        // ✅ Conectado (online)
                        iconConnected.visibility = View.VISIBLE
                        iconDisconnected.visibility = View.GONE
                        tvStatus.text = context.getString(R.string.online) // "en línea"
                        tvStatus.setTypeface(null, Typeface.NORMAL)
                        tvStatus.setTextColor(context.resources.getColor(R.color.colorClaro))
                    } else {
                        // ⚠️ Puede estar escribiendo / grabando / desconectado

                        if (estado != null &&
                            (estado == context.getString(R.string.escribiendo) ||
                                    estado == context.getString(R.string.grabando))
                        ) {
                            // 📝 Escribiendo / 🎙 Grabando
                            // Tenemos que chequear si el chat actual es conmigo

                            firebaseRefsContainer.refDatos.child(userId)
                                .child("ChatList")
                                .child("Actual")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(dsChat: DataSnapshot) {
                                        if (dsChat.exists()) {
                                            val currentChat =
                                                dsChat.getValue(String::class.java)
//                                            // usuario actual logueado
//                                            val myUid: String? = if (auth.currentUser != null)
//                                                auth.currentUser?.uid
//                                            else
//                                                null

                                            // compara: if (Actual == userLogged + type)
                                            if (currentChat != null && currentChat == sessionManager.user.uid + type) {
                                                // ✅ Mostrar “escribiendo” o “grabando”

                                                iconConnected.visibility = View.VISIBLE
                                                iconDisconnected.visibility = View.GONE
                                                tvStatus.text = estado
                                                tvStatus.setTypeface(null, Typeface.ITALIC)
                                                tvStatus.setTextColor(
                                                    context.resources.getColor(R.color.accent)
                                                )
                                            } else {
                                                // está escribiendo pero NO conmigo → mostrar online normal
                                                iconConnected.visibility = View.VISIBLE
                                                iconDisconnected.visibility = View.GONE
                                                tvStatus.text = context.getString(R.string.online)
                                                tvStatus.setTypeface(null, Typeface.NORMAL)
                                                tvStatus.setTextColor(
                                                    context.resources
                                                        .getColor(R.color.colorClaro)
                                                )
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // opcional: log
                                    }
                                })
                        } else {
                            // ⏱ Última vez conectado
                            iconConnected.visibility = View.GONE
                            iconDisconnected.visibility = View.VISIBLE
                            tvStatus.setTypeface(null, Typeface.NORMAL)
                            tvStatus.setTextColor(
                                context.resources.getColor(R.color.colorClaro)
                            )

                            if (fecha != null && fecha == dateFormat.format(c.getTime())) {
                                // hoy
                                tvStatus.text = context.getString(R.string.ultVez) + " " +
                                        context.getString(R.string.today) + " " +
                                        context.getString(R.string.a_las) + " " +
                                        hora
                            } else {
                                // quizá ayer
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.DATE, -1)

                                if (fecha != null && fecha == dateFormat.format(calendar.getTime())) {
                                    tvStatus.text = context.getString(R.string.ultVez) + " " +
                                            context.getString(R.string.yesterday) + " " +
                                            context.getString(R.string.a_las) + " " +
                                            hora
                                } else {
                                    // fecha cualquiera
                                    tvStatus.text = context.getString(R.string.ultVez) + " " +
                                            fecha + " " +
                                            context.getString(R.string.a_las) + " " +
                                            hora
                                }
                            }
                        }
                    }
                } else {
                    // no hay nodo "Estado" → mostrar desconectado
                    iconConnected.visibility = View.GONE
                    iconDisconnected.visibility = View.VISIBLE
                    tvStatus.text = context.getString(R.string.offline) // "disconnected" → desconectado
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // opcional: log
            }
        })
    }

    fun setNoLeido(idUser: String, type: String) {
        firebaseRefsContainer.refDatos.child(sessionManager.uid).child(type).child(idUser).child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataVistos: DataSnapshot) {
                    if (dataVistos.exists()) {
                        val noVistos = dataVistos.getValue(Int::class.java)

                        firebaseRefsContainer.refDatos.child(sessionManager.uid).child("ChatList").child("msgNoLeidos")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataLeidos: DataSnapshot) {
                                    if (dataLeidos.exists()) {
                                        val noLeidos = dataLeidos.getValue<Int?>(Int::class.java)
                                        val count = noLeidos!! - noVistos!!
                                        if ((noVistos > 0)) {
                                            dataVistos.ref.setValue(0)
                                            dataLeidos.ref.setValue(count)
                                        } else {
                                            dataVistos.ref.setValue(1)
                                            dataLeidos.ref.setValue(noLeidos + 1)
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }


    fun silent(nameUser: String?, idUser: String?, type: String?) {
        firebaseRefsContainer.refDatos.child(sessionManager.uid).child(type!!).child(idUser!!)
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
                firebaseRefsContainer.refDatos.child(sessionManager.uid).child(type).child(idUser)
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
                firebaseRefsContainer.refDatos.child(sessionManager.uid).child(type).child(idUser)
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

        firebaseRefsContainer.refDatos.child(sessionManager.uid).child(Constants.CHATWITH).child(user_id).child("estado")
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

        firebaseRefsContainer.refCuentas.child(sessionManager.uid).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    firebaseRefsContainer.refCuentas.child(sessionManager.uid).child("latitud").setValue(latitude)
                    firebaseRefsContainer.refCuentas.child(sessionManager.uid).child("longitud").setValue(
                        longitude
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    companion object
}