package com.zibete.proyecto1.domain.session

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.facebook.login.LoginManager
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.auth.AuthSessionActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface LogoutUseCase {
    suspend fun execute(): ZibeResult<Unit>
}

class DefaultLogoutUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepositoryActions: UserRepositoryActions,
    private val userPreferencesActions: UserPreferencesActions,
    private val authSessionActions: AuthSessionActions,
    private val sessionConflictMonitor: SessionConflictMonitor,
    private val loginManager: LoginManager
) : LogoutUseCase {

    override suspend fun execute(): ZibeResult<Unit> = zibeCatching {
        // 1. Detener monitoreo de conflicto
        sessionConflictMonitor.stop()
        // 2. Actualizar última conexión
        userRepositoryActions.setUserLastSeen()
        // 3. Limpiar sesión
        authSessionActions.signOutFirebaseUser().getOrThrow()
        loginManager.logOut()
        // 4. Limpiar Credential Manager
        try {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Throwable) {
        }
        // 5. Limpiar DataStore local
        userPreferencesActions.clearSessionData()
    }
}
