package com.zibete.proyecto1.domain.profile

import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.auth.AuthSessionActions
import jakarta.inject.Inject

interface UpdateEmailUseCase {
    suspend fun execute(newEmail: String): ZibeResult<Unit>
}

class DefaultUpdateEmailUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val authSessionActions: AuthSessionActions
) : UpdateEmailUseCase {

    override suspend fun execute(newEmail: String): ZibeResult<Unit> =
        zibeCatching {
            // 1. Firebase Auth
            authSessionActions.updateEmail(newEmail).getOrThrow()

            // 2. RTDB
            val updates = mapOf(
                Constants.AccountsKeys.EMAIL to newEmail
            )
            userRepositoryActions.updateUserFields(updates)

            // 3. Local
            userRepositoryActions.updateLocalProfile(null, null, newEmail)
        }
}