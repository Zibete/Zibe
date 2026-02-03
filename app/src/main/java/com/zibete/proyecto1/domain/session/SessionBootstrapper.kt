package com.zibete.proyecto1.domain.session

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import javax.inject.Inject

interface SessionBootstrapper {
    suspend fun bootstrap(
        uid: String,
        name: String = "",
        birthDate: String = "",
        description: String = "",
    ): ZibeResult<Unit>
}

class DefaultSessionBootstrapper @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
    private val sessionRepositoryActions: SessionRepositoryActions,
    private val sessionRepositoryProvider: SessionRepositoryProvider,
    private val sessionConflictMonitor: SessionConflictMonitor,
    private val userRepositoryActions: UserRepositoryActions,
    private val userRepositoryProvider: UserRepositoryProvider,
    private val userPreferencesActions: UserPreferencesActions
) : SessionBootstrapper {

    override suspend fun bootstrap(
        uid: String,
        name: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit> =
        zibeCatching {
            // 1. Registro de Sesión (FCM e InstallId)
            val installId = setActiveSession(uid)
            // 2. Monitoreo de Conflictos
            sessionConflictMonitor.start(uid = uid, installId = installId)
            // 3. Flujo de Usuario (Creación o Verificación)
            updateUserFlow(
                uid = uid,
                name = name,
                birthDate = birthDate,
                description = description
            )
            // 4. Actualización de Caché Local
            setLocalProfile()
        }

    private suspend fun setActiveSession(uid: String): String {
        val installId = sessionRepositoryProvider.getLocalInstallId()
        val fcmToken = sessionRepositoryProvider.getLocalFcmToken()

        sessionRepositoryActions.setActiveSession(
            uid = uid,
            installId = installId,
            fcmToken = fcmToken
        )
        return installId
    }


    private suspend fun updateUserFlow(
        uid: String,
        name: String,
        birthDate: String,
        description: String,
    ) {
        val accountExists = userRepositoryProvider.accountExists(uid)

        // Usuario nuevo
        if (!accountExists) {
            authSessionProvider.currentUser?.let { user: FirebaseUser ->
                val resolvedName = name.ifBlank { user.displayName.orEmpty() }
                userRepositoryActions.createUserNode(user, resolvedName, birthDate, description)
            }
            userPreferencesActions.setFirstLoginDone(false)
            return
        }

        // Perfil incompleto / completo
        val hasBirthDate = userRepositoryProvider.hasBirthDate(uid)
        userPreferencesActions.setFirstLoginDone(hasBirthDate)
    }

    private suspend fun setLocalProfile() {
        authSessionProvider.currentUser?.let { user: FirebaseUser ->
            userRepositoryActions.updateLocalProfile(
                name = user.displayName,
                photoUrl = user.photoUrl.toString(),
                email = user.email
            )
        }
    }
}
