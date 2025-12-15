package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer
) {



    suspend fun getLocalInstallId(): String = FirebaseInstallations.getInstance().id.await()

    suspend fun getLocalFcmToken(): String = FirebaseMessaging.getInstance().token.await()



    // ============================================================
    // Refs
    // ============================================================

    private fun sessionRef(uid: String) =
        firebaseRefsContainer.refSessions.child(uid)

    private fun activeInstallRef(uid: String) =
        sessionRef(uid).child("activeInstallId")

    private fun fcmTokenRef(uid: String) =
        sessionRef(uid).child("fcmToken")

    // ============================================================
    // WRITE
    // ============================================================

    suspend fun setActiveSession(
        uid: String,
        installId: String,
        fcmToken: String?
    ) {
        val data = mutableMapOf<String, Any>(
            "activeInstallId" to installId
        )

        if (!fcmToken.isNullOrBlank()) {
            data["fcmToken"] = fcmToken
        }

        sessionRef(uid)
            .updateChildren(data)
            .await()
    }

    suspend fun clearSession(uid: String) {
        sessionRef(uid)
            .removeValue()
            .await()
    }

    // ============================================================
    // READ (one-shot)
    // ============================================================

    suspend fun getActiveInstallId(uid: String): String? =
        activeInstallRef(uid)
            .get()
            .await()
            .getValue(String::class.java)

    suspend fun getFcmToken(uid: String): String? =
        fcmTokenRef(uid)
            .get()
            .await()
            .getValue(String::class.java)

    // ============================================================
    // LISTENER (conflicto de sesión)
    // ============================================================

    fun observeSessionConflict(
        uid: String,
        myInstallId: String,
        onConflict: () -> Unit
    ): ValueEventListener {

        val ref = activeInstallRef(uid)

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
        activeInstallRef(uid).removeEventListener(listener)
    }
}
