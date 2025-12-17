package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.constants.Constants.KEY_ACTIVE_INSTALL_ID
import com.zibete.proyecto1.ui.constants.Constants.KEY_FCM_TOKEN
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer
) {

    // LOCAL INFO
    suspend fun getLocalInstallId(): String = FirebaseInstallations.getInstance().id.await()
    suspend fun getLocalFcmToken(): String = FirebaseMessaging.getInstance().token.await()

    // ============================================================
    // Refs
    // ============================================================

    private fun refSession(uid: String) =
        firebaseRefsContainer.refSessions.child(uid)

    private fun refInstallId(uid: String) =
        refSession(uid).child(KEY_ACTIVE_INSTALL_ID)

    private fun refFcmToken(uid: String) =
        refSession(uid).child(KEY_FCM_TOKEN)

    // ============================================================
    // READ
    // ============================================================

    suspend fun getInstallId(uid: String): String? =
        refInstallId(uid)
            .get()
            .await()
            .getValue(String::class.java)

    suspend fun getFcmToken(uid: String): String? =
        refFcmToken(uid)
            .get()
            .await()
            .getValue(String::class.java)

    suspend fun findSessionsByFcmToken(token: String): DataSnapshot {
        return firebaseRefsContainer.refSessions
            .orderByChild(KEY_FCM_TOKEN)
            .equalTo(token)
            .get()
            .await()
    }

    // ============================================================
    // WRITE
    // ============================================================

    suspend fun setActiveSession(
        uid: String,
        installId: String,
        fcmToken: String?
    ) {
        val data = mutableMapOf<String, Any>(
            KEY_ACTIVE_INSTALL_ID to installId
        )

        if (!fcmToken.isNullOrBlank()) {
            data[KEY_FCM_TOKEN] = fcmToken
        }

        refSession(uid)
            .updateChildren(data)
            .await()
    }

    suspend fun clearSession(uid: String) {
        refSession(uid)
            .removeValue()
            .await()
    }

    // ============================================================
    // LISTENER (conflicto de sesión)
    // ============================================================

    fun observeSessionConflict(
        uid: String,
        myInstallId: String,
        onConflict: () -> Unit
    ): ValueEventListener {

        val ref = refInstallId(uid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteId = snapshot.getValue(String::class.java)
                if (!remoteId.isNullOrBlank() && remoteId != myInstallId) {
                    onConflict()
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        return listener
    }

    fun removeSessionListener(uid: String, listener: ValueEventListener) {
        refInstallId(uid).removeEventListener(listener)
    }
}
