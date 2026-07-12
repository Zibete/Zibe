package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.data.SessionConflictSubscription
import com.zibete.proyecto1.data.SessionRepositoryProvider
import javax.inject.Inject
import javax.inject.Singleton

interface SessionConflictMonitor {
    fun start(uid: String, installId: String)
    fun stop()
}

interface SessionConflictNavigator {
    fun onSessionConflict()
}

@Singleton
class DefaultSessionConflictMonitor @Inject constructor(
    private val sessionRepositoryProvider: SessionRepositoryProvider,
    private val sessionConflictNavigator: SessionConflictNavigator
) : SessionConflictMonitor {

    private var subscription: SessionConflictSubscription? = null

    override fun start(uid: String, installId: String) {
        stop()
        subscription = sessionRepositoryProvider.observeSessionConflict(
            uid = uid,
            myInstallId = installId
        ) {
            stop()
            sessionConflictNavigator.onSessionConflict()
        }
    }

    override fun stop() {
        subscription?.cancel()
        subscription = null
    }
}
