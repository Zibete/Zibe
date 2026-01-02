package com.zibete.proyecto1.domain.session

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface LogoutUseCase {
    suspend fun execute()
}

class DefaultLogoutUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val sessionActions: UserSessionActions,
    @ApplicationContext private val context: Context
) : LogoutUseCase {
    override suspend fun execute() {
        userRepositoryActions.setUserLastSeen()
        sessionActions.logOutCleanup()
        //Reset CM
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
