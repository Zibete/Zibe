package com.zibete.proyecto1.domain.session

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionProvider

import javax.inject.Inject

interface SessionBootstrapper {
    suspend fun bootstrap(uid: String) 
}

class DefaultSessionBootstrapper @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val sessionProvider: UserSessionProvider,
    private val preferencesActions: UserPreferencesActions
) : SessionBootstrapper {

    override suspend fun bootstrap(uid: String) {
        setActiveSession(uid)
        updateUserFlow(uid)
    }

    private suspend fun setActiveSession(uid: String) {
        val installId = sessionRepository.getLocalInstallId()
        val fcmToken = sessionRepository.getLocalFcmToken()

        sessionRepository.setActiveSession(
            uid = uid,
            installId = installId,
            fcmToken = fcmToken
        )
    }

    private suspend fun updateUserFlow(uid: String) {
        val snapshot = userRepository.getAccountSnapshot(uid)

        // Usuario nuevo
        if (!snapshot.exists()) {
            sessionProvider.currentUser?.let { user: FirebaseUser ->
                userRepository.createUserNode(user, "", "")
            }
            preferencesActions.setFirstLoginDone(false)
            return
        }

        // Perfil incompleto / completo
        val hasBirthDate = userRepository.hasBirthDate(uid)
        preferencesActions.setFirstLoginDone(hasBirthDate)
    }
}
