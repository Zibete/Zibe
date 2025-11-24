package com.zibete.proyecto1.utils

import android.app.AlertDialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs.refChats
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser

object ChatUtils {

    private val user get() = currentUser!!


    // ---------- OCULTAR CHAT ----------
    fun unhiddenChat(
        ctx: Context,
        idUser: String,
        nameUser: String,
        view: View,
        type: String
    ) {
        AlertDialog.Builder(ContextThemeWrapper(ctx, R.style.AlertDialogApp))
            .setTitle("Ocultar chat con $nameUser")
            .setPositiveButton("Aceptar") { _, _ ->
                refDatos.child(user.uid).child(type).child(idUser)
                    .child("estado").setValue("delete")

                val snack = Snackbar.make(view, "Se ha ocultado el chat", Snackbar.LENGTH_SHORT)
                snack.setBackgroundTint(ctx.getColor(R.color.colorC))
                snack.view.findViewById<TextView>(
                    com.google.android.material.R.id.snackbar_text
                ).textAlignment = View.TEXT_ALIGNMENT_CENTER
                snack.show()
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    // ---------- ELIMINAR CHAT ----------
    fun deleteChat(
        context: Context,
        idUser: String,
        nameUser: String?,
        view: View,
        type: String
    ) {
        val ref = if (type == Constants.CHATWITH) Constants.CHAT else Constants.UNKNOWN

        val refYourReceiverData: StorageReference = Constants.storageReference.child("$type/$idUser/")
        val refMyReceiverData: StorageReference = Constants.storageReference.child("$type/${user!!.uid}/")

        val startedByMe = refChats.child(ref).child("${user.uid} <---> $idUser").child("Mensajes")
        val startedByHim = refChats.child(ref).child("$idUser <---> ${user.uid}").child("Mensajes")

        startedByMe.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    handleMessages(snapshot, context, view, idUser, nameUser, type, refYourReceiverData, refMyReceiverData)
                } else {
                    startedByHim.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snap: DataSnapshot) {
                            if (snap.exists()) {
                                handleMessages(snap, context, view, idUser, nameUser, type, refYourReceiverData, refMyReceiverData)
                            } else {
                                showSnack(context, view, "Chat vacío")
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleMessages(
        dataSnapshot: DataSnapshot,
        context: Context,
        view: View,
        idUser: String,
        nameUser: String?,
        type: String,
        refYourReceiverData: StorageReference,
        refMyReceiverData: StorageReference
    ) {
        val messages = mutableListOf<Chats>()

        for (snap in dataSnapshot.children) {
            val chat = snap.getValue(Chats::class.java) ?: continue
            val isMine = chat.sender == user.uid

            if (isMine && chat.type in listOf(Constants.MSG, Constants.PHOTO, Constants.AUDIO, Constants.MSG_RECEIVER_DLT, Constants.PHOTO_RECEIVER_DLT, Constants.AUDIO_RECEIVER_DLT))
                messages.add(chat)
            if (!isMine && chat.type in listOf(Constants.MSG, Constants.PHOTO, Constants.AUDIO, Constants.MSG_SENDER_DLT, Constants.PHOTO_SENDER_DLT, Constants.AUDIO_SENDER_DLT))
                messages.add(chat)
        }

        val count = messages.size
        if (count == 0) {
            showSnack(context, view, "Chat vacío")
            return
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogApp))
        builder.setTitle("Eliminar chat con $nameUser")

        val titles = arrayOf(
            "Ocultar chat",
            if (count == 1) "Eliminar $count mensaje" else "Eliminar $count mensajes"
        )
        val itemSelected = intArrayOf(0)

        builder.setSingleChoiceItems(titles, 0) { _, idx -> itemSelected[0] = idx }

        builder.setPositiveButton("Aceptar") { _, _ ->
            if (itemSelected[0] == 0) {
                refDatos.child(user.uid).child(type).child(idUser)
                    .child("estado").setValue("delete")
                showSnack(context, view, "Se ha ocultado el chat")
            } else {
                deleteMessages(
                    dataSnapshot, messages,
                    refYourReceiverData, refMyReceiverData
                )
                refDatos.child(user.uid).child(type).child(idUser).removeValue()
                Toast.makeText(
                    context,
                    if (count == 1) "$count mensaje eliminado" else "$count mensajes eliminados",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.setCancelable(false)
        builder.show()
    }

    private fun deleteMessages(
        dataSnapshot: DataSnapshot,
        messages: List<Chats>,
        refYourReceiverData: StorageReference,
        refMyReceiverData: StorageReference
    ) {
        for (chat in messages) {
            dataSnapshot.ref.orderByChild("date").equalTo(chat.date)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(ds: DataSnapshot) {
                        for (snap in ds.children) {
                            val type = snap.child("type").getValue(Int::class.java)
                            val sender = snap.child("envia").getValue(String::class.java)

                            val isMine = sender == user.uid

                            when {
                                isMine && type == Constants.MSG -> snap.child("type").ref.setValue(
                                    Constants.MSG_SENDER_DLT)
                                !isMine && type == Constants.MSG -> snap.child("type").ref.setValue(
                                    Constants.MSG_RECEIVER_DLT)
                                isMine && type == Constants.PHOTO -> snap.child("type").ref.setValue(
                                    Constants.PHOTO_SENDER_DLT)
                                !isMine && type == Constants.PHOTO -> snap.child("type").ref.setValue(
                                    Constants.PHOTO_RECEIVER_DLT)
                                isMine && type == Constants.AUDIO -> snap.child("type").ref.setValue(
                                    Constants.AUDIO_SENDER_DLT)
                                !isMine && type == Constants.AUDIO -> snap.child("type").ref.setValue(
                                    Constants.AUDIO_RECEIVER_DLT)
                            }

                            // eliminar archivos multimedia remotos
                            if (!isMine && type in listOf(Constants.PHOTO_SENDER_DLT, Constants.AUDIO_SENDER_DLT)) {
                                deleteRemoteFile(refMyReceiverData, chat)
                                snap.ref.removeValue()
                            } else if (isMine && type in listOf(Constants.PHOTO_RECEIVER_DLT, Constants.AUDIO_RECEIVER_DLT)) {
                                deleteRemoteFile(refYourReceiverData, chat)
                                snap.ref.removeValue()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun deleteRemoteFile(ref: StorageReference, chat: Chats) {
        val msg = chat.message
        val start = msg.indexOf(user.uid) + user.uid.length + 3
        val ext = if (msg.contains(".jpg")) ".jpg" else ".mp3"
        val end = msg.indexOf(ext) + ext.length
        if (start in 0..end && end <= msg.length) {
            val fileName = msg.substring(start, end)
            ref.child(fileName).delete()
        }
    }

    private fun showSnack(ctx: Context, view: View, text: String) {
        val snack = Snackbar.make(view, text, Snackbar.LENGTH_SHORT)
        snack.setBackgroundTint(ctx.getColor(R.color.colorC))
        snack.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        ).textAlignment = View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }
}
