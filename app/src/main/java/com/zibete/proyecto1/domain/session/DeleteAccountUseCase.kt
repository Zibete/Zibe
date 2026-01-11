package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import javax.inject.Inject

interface DeleteAccountUseCase {
    suspend fun execute(): ZibeResult<Unit>
}
class DefaultDeleteAccountUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val userSessionActions: UserSessionActions
) : DeleteAccountUseCase {

    override suspend fun execute(): ZibeResult<Unit> =
        zibeCatching {
            // 1) Borrar datos (RTDB / Storage)
            userRepositoryActions.deleteMyAccountData().getOrThrow()
            // 2) Borrar de Auth (último paso)
            userSessionActions.deleteFirebaseUser().getOrThrow()
        }

}
