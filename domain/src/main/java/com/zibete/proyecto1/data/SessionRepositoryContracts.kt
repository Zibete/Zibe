package com.zibete.proyecto1.data

interface SessionRepositoryActions {
    suspend fun setActiveSession(uid: String, installId: String, fcmToken: String?)
    suspend fun clearSession(uid: String)
}

interface SessionRepositoryProvider {
    suspend fun getLocalInstallId(): String
    suspend fun getLocalFcmToken(): String?
    suspend fun getInstallId(uid: String): String?
    suspend fun getFcmToken(uid: String): String?
    suspend fun countSessionsByFcmToken(token: String): Long
    fun observeSessionConflict(
        uid: String,
        myInstallId: String,
        onConflict: () -> Unit
    ): SessionConflictSubscription
}

fun interface SessionConflictSubscription {
    fun cancel()
}
