package com.zibete.proyecto1.di.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.utils.FirebaseRefs

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Contenedor inyectable que agrupa todas las DatabaseReference utilizadas comúnmente
 * en la aplicación. Esto centraliza la inyección de múltiples @Named referencias.
 *
 * NOTA: Esta clase es inyectada por Hilt automáticamente si todas sus dependencias
 * (las referencias @Named) están definidas en el FirebaseModule.
 */
@Singleton
class FirebaseRefsContainer @Inject constructor(
    private val db: FirebaseDatabase,
    private val storage: FirebaseStorage
) {

    // === Referencias a Usuarios y Datos ===
    @Named("refUsuarios")     val refUsuarios: DatabaseReference = db.getReference("Usuarios")
    @Named("refDatos")        val refDatos: DatabaseReference = refUsuarios.child("Datos")
    @Named("refCuentas")      val refCuentas: DatabaseReference = refUsuarios.child("Cuentas")

    // === Referencias a Chats ===
    @Named("refChatsRoot")    val refChatsRoot: DatabaseReference = db.getReference("Chats")
    @Named("refChat")         val refChat: DatabaseReference = refChatsRoot.child("Chats")
    @Named("refChatUnknown")  val refChatUnknown: DatabaseReference = refChatsRoot.child("Unknown")

    // === Referencias a Grupos ===
    @Named("refGroupsRoot")   val refGroupsRoot: DatabaseReference = db.getReference("Groups")
    @Named("refGroupData")    val refGroupData: DatabaseReference = refGroupsRoot.child("Data")
    @Named("refGroupChat")    val refGroupChat: DatabaseReference = refGroupsRoot.child("Chat")
    @Named("refGroupUsers")   val refGroupUsers: DatabaseReference = refGroupsRoot.child("Users")

    // === Zibe ===
    @Named("refZibe")         val refZibe: DatabaseReference = db.getReference("Zibe")

    // === Storage ===
    @Named("firebaseStorage") val firebaseStorage: FirebaseStorage = storage
}




