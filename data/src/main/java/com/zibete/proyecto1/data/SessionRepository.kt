package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.core.constants.Constants.SessionKeys
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer
) : SessionRepositoryActions, SessionRepositoryProvider {

    // LOCAL INFO
    override suspend fun getLocalInstallId(): String = FirebaseInstallations.getInstance().id.await()
    override suspend fun getLocalFcmToken(): String? = FirebaseMessaging.getInstance().token.await()

    // ============================================================
    // READ
    // ============================================================

    override suspend fun getInstallId(uid: String): String? =
        refInstallId(uid)
            .get()
            .await()
            .getValue(String::class.java)

    override suspend fun getFcmToken(uid: String): String? =
        refFcmToken(uid)
            .get()
            .await()
            .getValue(String::class.java)

    override suspend fun getSessionsByFcmToken(token: String): DataSnapshot {
        return firebaseRefsContainer.refSessions
            .orderByChild(SessionKeys.FCM_TOKEN)
            .equalTo(token)
            .get()
            .await()
    }

    // ============================================================
    // WRITE
    // ============================================================

    override suspend fun setActiveSession(
        uid: String,
        installId: String,
        fcmToken: String?
    ) {
        val data = mutableMapOf<String, Any>(
            SessionKeys.ACTIVE_INSTALL_ID to installId
        )

        if (!fcmToken.isNullOrBlank()) {
            data[SessionKeys.FCM_TOKEN] = fcmToken
        }

        refSession(uid)
            .updateChildren(data)
            .await()
    }

    override suspend fun clearSession(uid: String) {
        refSession(uid)
            .removeValue()
            .await()
    }

    // ============================================================
    // LISTENER (conflicto de sesión)
    // ============================================================

    override fun observeSessionConflict(
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

    override fun removeSessionListener(uid: String, listener: ValueEventListener) {
        refInstallId(uid).removeEventListener(listener)
    }


    // ============================================================
    // Refs
    // ============================================================

    private fun refSession(uid: String) =
        firebaseRefsContainer.refSessions.child(uid)

    private fun refInstallId(uid: String) =
        refSession(uid).child(SessionKeys.ACTIVE_INSTALL_ID)

    private fun refFcmToken(uid: String) =
        refSession(uid).child(SessionKeys.FCM_TOKEN)
}
