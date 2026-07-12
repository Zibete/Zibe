package com.zibete.proyecto1.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.facebook.login.LoginManager
import com.zibete.proyecto1.domain.session.ExternalSessionCleaner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidExternalSessionCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loginManager: LoginManager
) : ExternalSessionCleaner {

    override suspend fun clear() {
        loginManager.logOut()
        CredentialManager.create(context)
            .clearCredentialState(ClearCredentialStateRequest())
    }
}
