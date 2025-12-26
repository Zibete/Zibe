package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import javax.inject.Inject

sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data class Failure(val cause: Throwable) : DeleteAccountResult
}

interface DeleteAccountUseCase {
    suspend fun execute(): DeleteAccountResult
}

class DefaultDeleteAccountUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val userSessionActions: UserSessionActions
) : DeleteAccountUseCase {

    override suspend fun execute(): DeleteAccountResult {
        return runCatching {
            // 1) borrar datos de la cuenta (RTDB / Storage / etc.)
            userRepositoryActions.deleteMyAccountData()

            // 2) borrar usuario de Firebase Auth (último paso)
            userSessionActions.deleteFirebaseUser()
        }.fold(
            onSuccess = { DeleteAccountResult.Success },
            onFailure = { DeleteAccountResult.Failure(it) }
        )
    }
}
