package com.zibete.proyecto1.domain.session

import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.data.SessionRepositoryProvider
import javax.inject.Inject
import javax.inject.Singleton

interface SessionConflictMonitor {
    fun start(uid: String, installId: String)
    fun stop()
}

@Singleton
class DefaultSessionConflictMonitor @Inject constructor(
    private val sessionRepositoryProvider: SessionRepositoryProvider,
    private val appNavigator: AppNavigator
) : SessionConflictMonitor {

    private var sessionListener: ValueEventListener? = null
    private var currentUid: String? = null

    override fun start(uid: String, installId: String) {
        stop()
        sessionListener = sessionRepositoryProvider.observeSessionConflict(
            uid = uid,
            myInstallId = installId
        ) {
            stop()
            appNavigator.finishFlowNavigateToSplash(sessionConflict = true)
        }
        currentUid = uid
    }

    override fun stop() {
        val uid = currentUid ?: return
        val listener = sessionListener ?: return
        sessionRepositoryProvider.removeSessionListener(uid, listener)
        sessionListener = null
        currentUid = null
    }
}
