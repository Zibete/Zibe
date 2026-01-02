package com.zibete.proyecto1.domain.session

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.ZibeResult
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.UserSessionProvider
import javax.inject.Inject

interface SessionBootstrapper {
    suspend fun bootstrap(
        uid: String,
        birthDate: String = "",
        description: String = "",
    ) : ZibeResult<Unit>
}

class DefaultSessionBootstrapper @Inject constructor(
    private val sessionRepositoryActions: SessionRepositoryActions,
    private val sessionRepositoryProvider: SessionRepositoryProvider,
    private val userRepositoryActions: UserRepositoryActions,
    private val userRepositoryProvider: UserRepositoryProvider,
    private val sessionProvider: UserSessionProvider,
    private val preferencesActions: UserPreferencesActions
) : SessionBootstrapper {

    override suspend fun bootstrap(
        uid: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit> {
        return try {
            setActiveSession(uid)
            updateUserFlow(
                uid = uid,
                birthDate = birthDate,
                description = description
            )
            ZibeResult.Success(Unit)
        } catch (e: Exception) {
            ZibeResult.Failure(e)
        }
    }

    private suspend fun setActiveSession(uid: String) {
        val installId = sessionRepositoryProvider.getLocalInstallId()
        val fcmToken = sessionRepositoryProvider.getLocalFcmToken()

        sessionRepositoryActions.setActiveSession(
            uid = uid,
            installId = installId,
            fcmToken = fcmToken
        )
    }

    private suspend fun updateUserFlow(
        uid: String,
        birthDate: String,
        description: String,
        ) {
        val accountExists = userRepositoryProvider.accountExists(uid)

        // Usuario nuevo
        if (!accountExists) {
            sessionProvider.currentUser?.let { user: FirebaseUser ->
                userRepositoryActions.createUserNode(user, birthDate, description)
            }
            preferencesActions.setFirstLoginDone(false)
            return
        }

        // Perfil incompleto / completo
        val hasBirthDate = userRepositoryProvider.hasBirthDate(uid)
        preferencesActions.setFirstLoginDone(hasBirthDate)
    }
}
