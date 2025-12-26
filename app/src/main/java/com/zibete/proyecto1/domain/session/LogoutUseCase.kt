package com.zibete.proyecto1.domain.session

import android.content.Intent
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import javax.inject.Inject

interface LogoutUseCase {
    suspend fun execute(): Intent
}

class DefaultLogoutUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val sessionActions: UserSessionActions
) : LogoutUseCase {
    override suspend fun execute(): Intent {
        userRepositoryActions.setUserLastSeen()
        return sessionActions.logOutCleanup()
    }
}
