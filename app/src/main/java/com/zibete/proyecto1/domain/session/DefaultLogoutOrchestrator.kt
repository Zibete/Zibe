package com.zibete.proyecto1.domain.session

import android.content.Intent
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserRepository
import javax.inject.Inject

interface LogoutOrchestrator {
    suspend fun execute(): Intent
}

class DefaultLogoutOrchestrator @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionActions: UserSessionActions
) : LogoutOrchestrator {
    override suspend fun execute(): Intent {
        userRepository.setUserLastSeen()
        return sessionActions.logOutCleanup()
    }
}
