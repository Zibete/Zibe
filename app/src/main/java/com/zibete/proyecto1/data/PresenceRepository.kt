package com.zibete.proyecto1.data

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.AccountsKeys
import com.zibete.proyecto1.core.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.core.constants.Constants.NODE_STATUS
import com.zibete.proyecto1.core.constants.Constants.NODE_USERS_ACCOUNTS
import com.zibete.proyecto1.core.constants.Constants.StatusKeys
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider
) {

    val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    val myUid: String
        get() = firebaseUser.uid

    private var connectedListener: ValueEventListener? = null

    private fun refConnected() =
        firebaseRefsContainer.firebaseDatabase.getReference(".info/connected")

    private fun statusRef(): DatabaseReference =
        firebaseRefsContainer.refData
            .child(myUid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_STATUS)

    private fun accountIsOnlineRef(): DatabaseReference =
        firebaseRefsContainer.refUsers
            .child(NODE_USERS_ACCOUNTS)
            .child(myUid)
            .child(AccountsKeys.IS_ONLINE)

    suspend fun startPresence() {
        val connectedRef = refConnected()
        val statusRef = statusRef()
        val accountOnlineRef = accountIsOnlineRef()

        statusRef.onDisconnect().setValue(
            mapOf(
                StatusKeys.STATUS to context.getString(R.string.ultVez),
                StatusKeys.LAST_SEEN_MS to ServerValue.TIMESTAMP
            )
        ).await()

        accountOnlineRef.onDisconnect().setValue(false).await()

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
        statusRef().setValue(Status(activity, 0L)).await()
        accountIsOnlineRef().setValue(true).await()
    }

    suspend fun setLastSeenNow() {
        statusRef().setValue(
            mapOf(
                StatusKeys.STATUS to context.getString(R.string.ultVez),
                StatusKeys.LAST_SEEN_MS to ServerValue.TIMESTAMP
            )
        ).await()
        accountIsOnlineRef().setValue(false).await()
    }

}
