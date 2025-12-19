package com.zibete.proyecto1.data

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.UserRepository.AccountKeys.IS_ONLINE
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Status
import com.zibete.proyecto1.ui.constants.Constants.NODE_STATUS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseRefsContainer: FirebaseRefsContainer
) {

    private var connectedListener: ValueEventListener? = null

    suspend fun startPresence(uid: String) {
        val connectedRef = firebaseRefsContainer.firebaseDatabase.getReference(".info/connected")

        val statusRef = firebaseRefsContainer.refData
            .child(uid)
            .child(NODE_STATUS)

        val accountOnlineRef = firebaseRefsContainer.refAccounts
            .child(uid)
            .child(IS_ONLINE)

        // ---- OFFLINE automático (si se cae conexión/proceso) ----
        statusRef.onDisconnect().setValue(
            mapOf(
                "status" to context.getString(R.string.ultVez),
                "lastSeenMs" to ServerValue.TIMESTAMP
            )
        ).await()

        accountOnlineRef.onDisconnect().setValue(false).await()

        // ---- ONLINE al conectar ----
        if (connectedListener != null) return

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (!connected) return

                statusRef.setValue(
                    Status(
                        status = context.getString(R.string.online),
                        lastSeenMs = 0L
                    )
                )

                accountOnlineRef.setValue(true)
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        connectedListener = listener
        connectedRef.addValueEventListener(listener)
    }

    suspend fun setActivityStatus(uid: String, activity: String) {
        val statusRef = firebaseRefsContainer.refData.child(uid).child(NODE_STATUS)
        // actividad => online implícito, lastSeen = 0
        statusRef.setValue(Status(activity, 0L)).await()

        firebaseRefsContainer.refAccounts
            .child(uid)
            .child(IS_ONLINE)
            .setValue(true)
            .await()
    }

    suspend fun setLastSeenNow(uid: String) {
        val statusRef = firebaseRefsContainer.refData.child(uid).child(NODE_STATUS)
        statusRef.setValue(
            mapOf(
                "status" to context.getString(R.string.ultVez),
                "lastSeenMs" to ServerValue.TIMESTAMP
            )
        ).await()

        firebaseRefsContainer.refAccounts
            .child(uid)
            .child(IS_ONLINE)
            .setValue(false)
            .await()
    }

    fun stopPresence() {
        val listener = connectedListener ?: return
        firebaseRefsContainer.firebaseDatabase
            .getReference(".info/connected")
            .removeEventListener(listener)
        connectedListener = null
    }
}
