package com.zibete.proyecto1.di.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT

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
    val storage: FirebaseStorage
) {

    // === Referencias a Usuarios ===
    @Named("refUsuarios")     val refUsers: DatabaseReference = db.getReference("Usuarios")
    @Named("refDatos")        val refData: DatabaseReference = refUsers.child("Datos")
    @Named("refCuentas")      val refAccounts: DatabaseReference = refUsers.child("Cuentas")

    // === Referencias a Chats ===
    @Named("refChatMessageRoot")    val refChatMessageRoot: DatabaseReference = db.getReference(NODE_CHATS)




    @Named("refChatMessageRoot")    val refChatMessageGroupsRoot: DatabaseReference = refChatMessageRoot.child(NODE_GROUP_CHAT)



//    @Named("refChat")         val refChat: DatabaseReference = refChatMessageRoot.child("ChatMessage") lo definimos con el type
//    @Named("refChatUnknown")  val refChatUnknown: DatabaseReference = refChatMessageRoot.child("Unknown")

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




