package com.zibete.proyecto1.model

import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference

data class ChatRefs(
    val startedByMe: DatabaseReference,
    val startedByHim: DatabaseReference,
    val refYourReceiverData: StorageReference,
    val refMyReceiverData: StorageReference,
    val refActual: DatabaseReference,
    val token: String?,
    val refChat: String,
    val refChatWith: String
)