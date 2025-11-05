package com.zibete.proyecto1.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

object FirebaseRefs {

    // === Auth ===
    @JvmField
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    @JvmField
    val user: FirebaseUser? = auth.currentUser
    // === Database ===
    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    // Usuarios
    @JvmField val refUsuarios: DatabaseReference = db.getReference("Usuarios")
    @JvmField val refDatos: DatabaseReference = refUsuarios.child("Datos")
    @JvmField val refCuentas: DatabaseReference = refUsuarios.child("Cuentas")

    // Chats
    @JvmField val refChats: DatabaseReference = db.getReference("Chats")
    @JvmField val refChat: DatabaseReference = refChats.child("Chats")
    @JvmField val refChatUnknown: DatabaseReference = refChats.child("Unknown")

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
