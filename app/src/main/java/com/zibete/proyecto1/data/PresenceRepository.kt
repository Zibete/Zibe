package com.zibete.proyecto1.data

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Status
import com.zibete.proyecto1.ui.constants.Constants.StatusKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {

    private var connectedListener: ValueEventListener? = null

    private fun refConnected() =
        firebaseRefsContainer.firebaseDatabase.getReference(".info/connected")

    suspend fun startPresence() {
        val connectedRef = refConnected()

        val statusRef = userRepository.statusRef()

        val accountOnlineRef = userRepository.accountIsOnlineRef()

        statusRef.onDisconnect().setValue(
            mapOf(
                StatusKeys.STATUS to context.getString(R.string.ultVez),
                StatusKeys.LAST_SEEN_MS to ServerValue.TIMESTAMP
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

    fun stopPresence() {
        val connectedRef = refConnected()
        val listener = connectedListener ?: return
        connectedRef.removeEventListener(listener)
        connectedListener = null
    }

    suspend fun setActivityStatus(activity: String) {
        userRepository.statusRef().setValue(Status(activity, 0L)).await()
        userRepository.accountIsOnlineRef().setValue(true).await()
    }

    suspend fun setLastSeenNow() {
        userRepository.statusRef().setValue(
            mapOf(
                StatusKeys.STATUS to context.getString(R.string.ultVez),
                StatusKeys.LAST_SEEN_MS to ServerValue.TIMESTAMP
            )
        ).await()
        userRepository.accountIsOnlineRef().setValue(false).await()
    }

}
