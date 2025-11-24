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
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.adapters.AdapterPhotoReceived
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.DateUtils.calcAge
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

object ProfileUiBinder {

    private val user get() = currentUser!!


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

    // === Distancia (texto listo para UI) ===
    fun getDistanceToUser(idUser: String, distanceView: TextView) {
        FirebaseRefs.refCuentas.child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val otherLat = snapshot.child("latitud").getValue(Double::class.java)
                    val otherLng = snapshot.child("longitud").getValue(Double::class.java)

                    if (otherLat == null || otherLng == null) return

                    val distanceMeters = getDistanceMeters(
                        UserRepository.latitude,
                        UserRepository.longitude,
                        otherLat,
                        otherLng
                    )

                    distanceView.text = formatDistance(distanceMeters)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters > 10_000 -> {
                val km = distanceMeters / 1000
                val bd = BigDecimal(km).setScale(0, RoundingMode.HALF_UP)
                "A $bd kilómetros"
            }
            distanceMeters > 1_000 -> {
                val km = distanceMeters / 1000
                val bd = BigDecimal(km).setScale(1, RoundingMode.HALF_UP)
                "A $bd kilómetros"
            }
            else -> {
                val bd = BigDecimal(distanceMeters).setScale(0, RoundingMode.HALF_UP)
                "A $bd metros"
            }
        }
    }

    // === Menú flotante de perfil (chat normal / chat incógnito) ===
    fun setMenuProfile(
        context: Context,
        idUser: String,
        subMenuChatWithUnknown: FloatingActionButton,
        subMenuChatWith: FloatingActionButton
    ) {
        // Chat incógnito (si está en grupo)
        FirebaseRefs.refGroupUsers.child(UsuariosFragment.groupName).child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val unknownName =
                            snapshot.child("user_name").getValue(String::class.java)

                        subMenuChatWithUnknown.labelText =
                            "Chat privado de: ${UsuariosFragment.groupName}"

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
        FirebaseRefs.refDatos.child(user.uid).child("FavoriteList").child(userId)
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
        FirebaseRefs.refDatos.child(user.uid).child(Constants.CHATWITH).child(userId).child("estado")
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
        FirebaseRefs.refDatos.child(userId).child(Constants.CHATWITH).child(user!!.uid).child("estado")
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
        FirebaseRefs.refChat.child("${user.uid} <---> $idUser").child("Mensajes")
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
        FirebaseRefs.refChat.child("$idUser <---> ${user.uid}").child("Mensajes")
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

        if (sender != user.uid && url != null && (type == Constants.PHOTO || type == Constants.PHOTO_SENDER_DLT)) {
            adapter.addString(url)
            linearPhotos.visibility = View.VISIBLE
        }
    }

    // === Distancia en metros (Haversine / coseno esférico simplificado) ===
    fun getDistanceMeters(
        myLatitude: Double,
        myLongitude: Double,
        otherLatitude: Double,
        otherLongitude: Double
    ): Double {
        val lat1 = Math.toRadians(myLatitude)
        val lat2 = Math.toRadians(otherLatitude)
        val lon1 = Math.toRadians(myLongitude)
        val lon2 = Math.toRadians(otherLongitude)

        var dist = acos(
            sin(lat1) * sin(lat2) +
                    cos(lat1) * cos(lat2) * cos(lon1 - lon2)
        )

        if (dist < 0) {
            dist += Math.PI
        }

        // Radio aproximado de la Tierra en metros
        return (dist * 6_378_100).roundToLong().toDouble()
    }
}
