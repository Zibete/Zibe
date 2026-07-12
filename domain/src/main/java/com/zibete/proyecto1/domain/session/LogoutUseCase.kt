package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.auth.AuthSessionActions
import javax.inject.Inject

interface LogoutUseCase {
    suspend fun execute(): ZibeResult<Unit>
}

interface ExternalSessionCleaner {
    suspend fun clear()
}

class DefaultLogoutUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val userPreferencesActions: UserPreferencesActions,
    private val authSessionActions: AuthSessionActions,
    private val sessionConflictMonitor: SessionConflictMonitor,
    private val externalSessionCleaner: ExternalSessionCleaner
) : LogoutUseCase {

    override suspend fun execute(): ZibeResult<Unit> = zibeCatching {
        // 1. Detener monitoreo de conflicto
        sessionConflictMonitor.stop()
        // 2. Actualizar última conexión
        userRepositoryActions.setUserLastSeen()
        // 3. Limpiar sesión
        authSessionActions.signOutFirebaseUser().getOrThrow()
        externalSessionCleaner.clear()
        // 5. Limpiar DataStore local
        userPreferencesActions.clearSessionData()
    }
}
