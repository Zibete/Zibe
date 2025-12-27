package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import javax.inject.Inject

interface LogoutUseCase {
    suspend fun execute()
}

class DefaultLogoutUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val sessionActions: UserSessionActions
) : LogoutUseCase {
    override suspend fun execute() {
        userRepositoryActions.setUserLastSeen()
        sessionActions.logOutCleanup()
    }
}
