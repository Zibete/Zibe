package com.zibete.proyecto1.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.location.Location
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.State
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.user
import java.text.SimpleDateFormat
import java.util.Calendar

// Clase que maneja la comunicación con Firebase para datos de usuario
// (repository = repositorio → capa de acceso a datos)
object UserRepository {
    // ------------------------------------------------------------
    // setUserOnline → establece al usuario como conectado
    // (StateOnLine = poner en línea)
    // ------------------------------------------------------------
    @JvmStatic
    fun setUserOnline(context: Context, id_user: String) {
        // 🔸 Crear el objeto Estado con los textos de recursos
        // Estado = clase de modelo que representa el estado de conexión

        val currentState = State(
            context.getString(R.string.conectado),  // "connected" → conectado
            "",
            ""
        )

        // 🔸 Guardar el estado en Firebase (dos ubicaciones distintas)
        refDatos.child(id_user).child("Estado").setValue(currentState)
        refCuentas.child(id_user).child("estado").setValue(true)
    }

    @JvmStatic
    fun setUserOffline(context: Context, id_user: String) {
        if (user != null) {
            val c = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm")
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")

            val cState = State(
                context.getString(R.string.ultVez),
                dateFormat.format(c.getTime()),
                timeFormat.format(c.getTime())
            )

            refDatos.child(id_user).child("Estado").setValue(cState)
            refCuentas.child(id_user).child("estado").setValue(false)
        }
    }

    @JvmStatic
    fun stateUser(
        context: Context,
        userId: String,  // id_user → usuario a mostrar
        iconConnected: ImageView,  // icon_conectado
        iconDisconnected: ImageView,  // icon_desconectado
        tvStatus: TextView,  // tv_estado
        type: String? // type → ej. "chatWith"
    ) {
        refDatos.child(userId).child("Estado").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val c = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy")

                    val estado = dataSnapshot.child("estado")
                        .getValue<String?>(String::class.java) // estado → "conectado", "escribiendo", etc.
                    val fecha = dataSnapshot.child("fecha")
                        .getValue<String?>(String::class.java) // fecha última vez
                    val hora = dataSnapshot.child("hora")
                        .getValue<String?>(String::class.java) // hora última vez

                    if (estado != null && estado == context.getString(R.string.conectado)) {
                        // ✅ Conectado (online)
                        iconConnected.setVisibility(View.VISIBLE)
                        iconDisconnected.setVisibility(View.GONE)
                        tvStatus.setText(context.getString(R.string.enlinea)) // "en línea"
                        tvStatus.setTypeface(null, Typeface.NORMAL)
                        tvStatus.setTextColor(context.getResources().getColor(R.color.colorClaro))
                    } else {
                        // ⚠️ Puede estar escribiendo / grabando / desconectado

                        if (estado != null &&
                            (estado == context.getString(R.string.escribiendo) ||
                                    estado == context.getString(R.string.grabando))
                        ) {
                            // 📝 Escribiendo / 🎙 Grabando
                            // Tenemos que chequear si el chat actual es conmigo

                            refDatos.child(userId)
                                .child("ChatList")
                                .child("Actual")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(dsChat: DataSnapshot) {
                                        if (dsChat.exists()) {
                                            val currentChat =
                                                dsChat.getValue<String?>(String::class.java)
                                            // usuario actual logueado
                                            val myUid: String? = if (auth.currentUser != null)
                                                auth.currentUser?.uid
                                            else
                                                null

                                            // compara: if (Actual == userLogged + type)
                                            if (myUid != null && currentChat != null && currentChat == myUid + type) {
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
                                                tvStatus.text = context.getString(R.string.enlinea)
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
                            iconConnected.setVisibility(View.GONE)
                            iconDisconnected.setVisibility(View.VISIBLE)
                            tvStatus.setTypeface(null, Typeface.NORMAL)
                            tvStatus.setTextColor(
                                context.getResources().getColor(R.color.colorClaro)
                            )

                            if (fecha != null && fecha == dateFormat.format(c.getTime())) {
                                // hoy
                                tvStatus.setText(
                                    context.getString(R.string.ultVez) + " " +
                                            context.getString(R.string.today) + " " +
                                            context.getString(R.string.a_las) + " " +
                                            hora
                                )
                            } else {
                                // quizá ayer
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.DATE, -1)

                                if (fecha != null && fecha == dateFormat.format(calendar.getTime())) {
                                    tvStatus.setText(
                                        context.getString(R.string.ultVez) + " " +
                                                context.getString(R.string.yesterday) + " " +
                                                context.getString(R.string.a_las) + " " +
                                                hora
                                    )
                                } else {
                                    // fecha cualquiera
                                    tvStatus.setText(
                                        context.getString(R.string.ultVez) + " " +
                                                fecha + " " +
                                                context.getString(R.string.a_las) + " " +
                                                hora
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // no hay nodo "Estado" → mostrar desconectado
                    iconConnected.setVisibility(View.GONE)
                    iconDisconnected.setVisibility(View.VISIBLE)
                    tvStatus.setText(context.getString(R.string.desconectado)) // "disconnected" → desconectado
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // opcional: log
            }
        })
    }

    fun setNoLeido(id_user: String, type: String) {
        refDatos.child(user!!.uid).child(type).child(id_user).child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataVistos: DataSnapshot) {
                    if (dataVistos.exists()) {
                        val noVistos = dataVistos.getValue<Int?>(Int::class.java)

                        refDatos.child(user.uid).child("ChatList").child("msgNoLeidos")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataLeidos: DataSnapshot) {
                                    if (dataLeidos.exists()) {
                                        val noLeidos = dataLeidos.getValue<Int?>(Int::class.java)
                                        val count = noLeidos!! - noVistos!!
                                        if ((noVistos > 0)) {
                                            dataVistos.getRef().setValue(0)
                                            dataLeidos.getRef().setValue(count)
                                        } else {
                                            dataVistos.getRef().setValue(1)
                                            dataLeidos.getRef().setValue(noLeidos + 1)
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

    @JvmStatic
    fun Silent(name_user: String?, id_user: String?, type: String?) {
        refDatos.child(user!!.uid).child(type!!).child(id_user!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val state =
                            dataSnapshot.child("estado").getValue<String?>(String::class.java)
                        val photo =
                            dataSnapshot.child("wUserPhoto").getValue<String?>(String::class.java)

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
                        newChatWith(dataSnapshot, id_user, name_user!!, "silent")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun newChatWith(dataSnapshot: DataSnapshot, id_user: String, name_user: String, state: String) {
        val c = Calendar.getInstance()
        val dateFormat3 = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")
        val newChat = ChatWith(
            "",
            dateFormat3.format(c.getTime()),
            null,
            "",
            id_user,
            name_user,
            Constants.EMPTY,
            state,
            0,
            1
        )

        dataSnapshot.getRef().setValue(newChat)
    }

    @JvmStatic
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
                refDatos.child(user!!.uid).child(type).child(idUser)
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


    @JvmStatic
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
                refDatos.child(user!!.uid).child(type).child(idUser)
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

        refDatos.child(user!!.uid).child(Constants.CHATWITH).child(user_id).child("estado")
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

    @JvmField
    var latitude: Double = 0.0
    @JvmField
    var longitude: Double = 0.0
    @JvmStatic
    fun updateLocationUI(mLastLocation: Location?) {
        if (mLastLocation == null) return

        latitude = mLastLocation.latitude
        longitude = mLastLocation.longitude

        refCuentas.child(user!!.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    FirebaseRefs.refCuentas.child(user.uid).child("latitud").setValue(latitude)
                    FirebaseRefs.refCuentas.child(user.uid).child("longitud").setValue(
                        longitude
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
