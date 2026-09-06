package com.zibete.proyecto1.data

import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.core.constants.Constants.SessionKeys
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class SessionRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    @param:ApplicationContext private val context: Context
) : SessionRepositoryActions, SessionRepositoryProvider {

    // LOCAL INFO
    private val isLocalBackend: Boolean
        get() {
            val local = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).metaData?.getBoolean(LOCAL_BACKEND_METADATA) == true
            val projectId = firebaseRefsContainer.firebaseDatabase.app.options.projectId
            if (local || context.packageName.endsWith(".local") || projectId?.startsWith("demo-") == true) {
                check(local) { "Local session metadata is missing; remote device registration refused" }
                check(context.packageName.endsWith(".local")) { "Local session requires an isolated app" }
                check(projectId == "demo-zibe-rooms") {
                    "Local session requires the demo backend"
                }
            }
            return local
        }

    override suspend fun getLocalInstallId(): String = if (isLocalBackend) {
        synchronized(this) {
            val preferences = context.getSharedPreferences("local_backend_session", Context.MODE_PRIVATE)
            preferences.getString("installation_id", null) ?: UUID.randomUUID().toString().also { id ->
                check(preferences.edit().putString("installation_id", id).commit()) {
                    "Could not persist the local installation identity"
                }
            }
        }
    } else {
        FirebaseInstallations.getInstance().id.await()
    }

    override suspend fun getLocalFcmToken(): String? =
        if (isLocalBackend) null else FirebaseMessaging.getInstance().token.await()

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

    override suspend fun countSessionsByFcmToken(token: String): Long =
        firebaseRefsContainer.refSessions
            .orderByChild(SessionKeys.FCM_TOKEN)
            .equalTo(token)
            .get()
            .await()
            .childrenCount

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
            .updateChildren(
                mapOf(
                    SessionKeys.ACTIVE_INSTALL_ID to null,
                    SessionKeys.FCM_TOKEN to null
                )
            )
            .await()
    }

    // ============================================================
    // LISTENER (conflicto de sesión)
    // ============================================================

    override fun observeSessionConflict(
        uid: String,
        myInstallId: String,
        onConflict: () -> Unit
    ): SessionConflictSubscription {

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
        return SessionConflictSubscription { ref.removeEventListener(listener) }
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

    private companion object {
        const val LOCAL_BACKEND_METADATA = "com.zibete.proyecto1.LOCAL_BACKEND"
    }
}
