package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener

interface SessionRepositoryActions {
    suspend fun setActiveSession(uid: String, installId: String, fcmToken: String?)
    suspend fun clearSession(uid: String)
}

interface SessionRepositoryProvider {
    suspend fun getLocalInstallId(): String
    suspend fun getLocalFcmToken(): String?
    suspend fun getInstallId(uid: String): String?
    suspend fun getFcmToken(uid: String): String?
    suspend fun getSessionsByFcmToken(token: String): DataSnapshot
    fun observeSessionConflict(
        uid: String,
        myInstallId: String,
        onConflict: () -> Unit
    ): ValueEventListener

    fun removeSessionListener(uid: String, listener: ValueEventListener)
}
