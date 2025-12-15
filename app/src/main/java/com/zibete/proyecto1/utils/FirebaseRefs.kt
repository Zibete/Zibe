package com.zibete.proyecto1.utils

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseRefs {


    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    suspend fun Query.awaitSnapshot() = get().await()

    // Usuarios
    @JvmField val refUsuarios: DatabaseReference = db.getReference("Usuarios")
    @JvmField val refDatos: DatabaseReference = refUsuarios.child("Datos")
    @JvmField val refCuentas: DatabaseReference = refUsuarios.child("Cuentas")

    // ChatMessage
    @JvmField val refChatMessage: DatabaseReference = db.getReference("ChatMessage")
    @JvmField val refChat: DatabaseReference = refChatMessage.child("ChatMessage")
    @JvmField val refChatUnknown: DatabaseReference = refChatMessage.child("Unknown")

    // Groups
    @JvmField val refGroups: DatabaseReference = db.getReference("Groups")
    @JvmField val refGroupData: DatabaseReference = refGroups.child("Data")
    @JvmField val refGroupChat: DatabaseReference = refGroups.child("Chat")
    @JvmField val refGroupUsers: DatabaseReference = refGroups.child("Users")

    // Zibe
    @JvmField val refZibe: DatabaseReference = db.getReference("Zibe")

    // === Storage ===
    @JvmField
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

}
