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
 * en la aplicación. Centraliza el acceso a Realtime Database y Storage.
 *
 * NOTA:
 * - `database` se expone para casos especiales como `.info/connected` (presencia).
 * - El resto de referencias mantiene compatibilidad total con el código existente.
 */
@Singleton
class FirebaseRefsContainer @Inject constructor(
    val firebaseDatabase: FirebaseDatabase,
    val firebaseStorage: FirebaseStorage
) {

    // ================= Usuarios =================

    @Named("refUsuarios")
    val refUsers: DatabaseReference =
        firebaseDatabase.getReference("Usuarios")

    @Named("refDatos")
    val refData: DatabaseReference =
        refUsers.child("Datos")

    @Named("refCuentas")
    val refAccounts: DatabaseReference =
        refUsers.child("Cuentas")

    // ================= Chats =================

    @Named("refChatMessageRoot")
    val refChatMessageRoot: DatabaseReference =
        firebaseDatabase.getReference(NODE_CHATS)

    @Named("refChatMessageGroupsRoot")
    val refChatMessageGroupsRoot: DatabaseReference =
        refChatMessageRoot.child(NODE_GROUP_CHAT)

    // ================= Sesiones =================

    val refSessions: DatabaseReference =
        firebaseDatabase.reference.child("sessions")

    // ================= Grupos =================

    @Named("refGroupsRoot")
    val refGroupsRoot: DatabaseReference =
        firebaseDatabase.getReference("Groups")

    @Named("refGroupData")
    val refGroupData: DatabaseReference =
        refGroupsRoot.child("Data")

    @Named("refGroupChat")
    val refGroupChat: DatabaseReference =
        refGroupsRoot.child("Chat")

    @Named("refGroupUsers")
    val refGroupUsers: DatabaseReference =
        refGroupsRoot.child("Users")

    // ================= Zibe =================

    @Named("refZibe")
    val refZibe: DatabaseReference =
        firebaseDatabase.getReference("Zibe")

}
