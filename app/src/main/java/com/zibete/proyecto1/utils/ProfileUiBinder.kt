package com.zibete.proyecto1.utils

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.clans.fab.FloatingActionButton
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.AdapterPhotoReceived
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.Utils.calcAge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileUiBinder @Inject constructor(
    private val repo: UserPreferencesRepository,
    private val sessionManager: UserSessionManager
) {

    private val myUid
        get() = sessionManager.myUid


    // === Edad ===
    fun getAge(idUser: String, ageView: TextView) {
        FirebaseRefs.refCuentas.child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val birthDay = snapshot.child("birthDay").getValue(String::class.java)
                    val edad = calcAge(birthDay)
                    ageView.text = edad.toString()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // === Menú flotante de perfil (chat normal / chat incógnito) ===
    fun setMenuProfile(
        context: Context,
        idUser: String,
        subMenuChatWithUnknown: FloatingActionButton,
        subMenuChatWith: FloatingActionButton
    ) {
        // Chat incógnito (si está en grupo)
        FirebaseRefs.refGroupUsers.child(repo.groupName).child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val unknownName =
                            snapshot.child("user_name").getValue(String::class.java)

                        subMenuChatWithUnknown.labelText =
                            "Chat privado de: ${repo.groupName}"

                        subMenuChatWithUnknown.setOnClickListener {
                            val intent = Intent(context, ChatActivity::class.java).apply {
                                putExtra("unknownName", unknownName)
                                putExtra("idUserUnknown", idUser)
                                addFlags(
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }
                            context.startActivity(intent)
                        }
                    } else {
                        subMenuChatWithUnknown.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Chat normal
        subMenuChatWith.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("id_user", idUser)
                addFlags(
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            context.startActivity(intent)
        }
    }

    // === Favoritos ===
    fun setFavorite(
        userId: String,
        favOn: ImageView,
        favOff: ImageView
    ) {
        FirebaseRefs.refDatos.child(myUid).child("FavoriteList").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val exists = snapshot.exists()
                    favOn.visibility = if (exists) View.VISIBLE else View.GONE
                    favOff.visibility = if (exists) View.GONE else View.VISIBLE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // === Bloqueo (yo bloqueo al otro) ===
    fun setBloq(
        userId: String,
        blockIcon: ImageView
    ) {
        FirebaseRefs.refDatos.child(myUid).child(Constants.NODE_CURRENT_CHAT).child(userId).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isBlocked = snapshot.getValue(String::class.java) == "bloq"
                    blockIcon.visibility = if (isBlocked) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // === Bloqueo (el otro me bloqueó) ===
    fun getBloqMe(
        userId: String,
        blockMeIcon: ImageView
    ) {
        FirebaseRefs.refDatos.child(userId).child(Constants.NODE_CURRENT_CHAT).child(myUid).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val blocked = snapshot.getValue(String::class.java) == "bloq"
                    blockMeIcon.visibility = if (blocked) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // === Fotos recibidas en perfil ===
    fun addPhotoReceived(
        idUser: String?,
        adapter: AdapterPhotoReceived,
        linearPhotos: LinearLayout
    ) {
        if (idUser.isNullOrEmpty()) return

        // Mis mensajes hacia él
        FirebaseRefs.refChat.child("${myUid} <---> $idUser").child("Mensajes")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    addPhoto(snapshot, adapter, linearPhotos)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })

        // Sus mensajes hacia mí
        FirebaseRefs.refChat.child("$idUser <---> ${myUid}").child("Mensajes")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    addPhoto(snapshot, adapter, linearPhotos)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addPhoto(
        snapshot: DataSnapshot,
        adapter: AdapterPhotoReceived,
        linearPhotos: LinearLayout
    ) {
        if (!snapshot.exists()) return

        val type = snapshot.child("type").getValue(Int::class.java)
        val sender = snapshot.child("envia").getValue(String::class.java)
        val url = snapshot.child("mensaje").getValue(String::class.java)

        if (sender != myUid && url != null && (type == Constants.MSG_PHOTO || type == Constants.MSG_PHOTO_SENDER_DLT)) {
            adapter.addString(url)
            linearPhotos.visibility = View.VISIBLE
        }
    }


}
