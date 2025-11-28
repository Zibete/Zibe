package com.zibete.proyecto1.di.firebase

import com.google.firebase.database.DatabaseReference
import com.zibete.proyecto1.utils.FirebaseRefs

import javax.inject.Inject
import javax.inject.Named

/**
 * Contenedor inyectable que agrupa todas las DatabaseReference utilizadas comúnmente
 * en la aplicación. Esto centraliza la inyección de múltiples @Named referencias.
 *
 * NOTA: Esta clase es inyectada por Hilt automáticamente si todas sus dependencias
 * (las referencias @Named) están definidas en el FirebaseModule.
 */
class FirebaseRefsContainer @Inject constructor(
    // === Referencias a Usuarios y Datos ===
    @Named("refUsuarios") val refUsuarios: DatabaseReference,
    @Named("refDatos") val refDatos: DatabaseReference,
    @Named("refCuentas") val refCuentas: DatabaseReference,

    // === Referencias a Chats ===
    @Named("refChat") val refChat: DatabaseReference,
    @Named("refChatUnknown") val refChatUnknown: DatabaseReference,

    // === Referencias a Grupos ===
    @Named("refGroupChat") val refGroupChat: DatabaseReference,
    @Named("refGroupUsers") val refGroupUsers: DatabaseReference
    // Puedes inyectar más referencias aquí (ej: refZibe) si son necesarias en tu Manager.



//
//// Chats
//@JvmField val refChats: DatabaseReference = db.getReference("Chats")
//@JvmField val refChat: DatabaseReference = refChats.child("Chats")
//@JvmField val refChatUnknown: DatabaseReference = refChats.child("Unknown")
//
//// Groups
//@JvmField val refGroups: DatabaseReference = db.getReference("Groups")
//@JvmField val refGroupData: DatabaseReference = refGroups.child("Data")
//@JvmField val refGroupChat: DatabaseReference = refGroups.child("Chat")
//@JvmField val refGroupUsers: DatabaseReference = refGroups.child("Users")
//
//// Zibe
//@JvmField val refZibe: DatabaseReference = db.getReference("Zibe")





)