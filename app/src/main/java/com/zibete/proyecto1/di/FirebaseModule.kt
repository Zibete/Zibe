package com.zibete.proyecto1.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // === 1. PROVEER AUTH ===

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // El usuario se obtiene del FirebaseAuth (no es Singleton)
    @Provides
    fun provideCurrentUser(auth: FirebaseAuth): FirebaseUser? = auth.currentUser

    // === 2. PROVEER BASE DE DATOS ===

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    // Proveer la referencia raíz a Usuarios (refUsuarios)
    @Provides
    @Singleton
    @Named("refUsuarios")
    fun provideRefUsuarios(db: FirebaseDatabase): DatabaseReference = db.getReference("Usuarios")

    // Proveer la referencia a Datos (refDatos)
    @Provides
    @Singleton
    @Named("refDatos")
    fun provideRefDatos(db: FirebaseDatabase): DatabaseReference = db.getReference("Usuarios").child("Datos")

    // Proveer la referencia a Cuentas (refCuentas)
    @Provides
    @Singleton
    @Named("refCuentas")
    fun provideRefCuentas(db: FirebaseDatabase): DatabaseReference = db.getReference("Usuarios").child("Cuentas")

    // Proveer la referencia a Chats (raíz)
    @Provides
    @Singleton
    @Named("refChatsRoot")
    fun provideRefChatsRoot(db: FirebaseDatabase): DatabaseReference = db.getReference("Chats")

    // Proveer la referencia a Chats (conversaciones)
    @Provides
    @Singleton
    @Named("refChat")
    fun provideRefChat(db: FirebaseDatabase): DatabaseReference = db.getReference("Chats").child("Chats")

    // Proveer la referencia a Chats Unknown (refChatUnknown)
    @Provides
    @Singleton
    @Named("refChatUnknown")
    fun provideRefChatUnknown(db: FirebaseDatabase): DatabaseReference = db.getReference("Chats").child("Unknown")

    // Proveer la referencia a Groups Chat (refGroupChat)
    @Provides
    @Singleton
    @Named("refGroupChat")
    fun provideRefGroupChat(db: FirebaseDatabase): DatabaseReference = db.getReference("Groups").child("Chat")

    // Proveer la referencia a Groups Users (refGroupUsers)
    @Provides
    @Singleton
    @Named("refGroupUsers")
    fun provideRefGroupUsers(db: FirebaseDatabase): DatabaseReference = db.getReference("Groups").child("Users")

    // === 3. PROVEER STORAGE ===

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}