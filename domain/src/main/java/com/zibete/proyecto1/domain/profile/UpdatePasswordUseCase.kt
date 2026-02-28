package com.zibete.proyecto1.domain.profile

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.AuthSessionActions
import jakarta.inject.Inject

interface UpdatePasswordUseCase {
    suspend fun execute(newPassword: String): ZibeResult<Unit>
}

class DefaultUpdatePasswordUseCase @Inject constructor(
    private val authSessionActions: AuthSessionActions
) : UpdatePasswordUseCase {

    override suspend fun execute(newPassword: String): ZibeResult<Unit> =
        zibeCatching {
            // 1. Firebase Auth
            authSessionActions.updatePassword(newPassword).getOrThrow()
        }
}