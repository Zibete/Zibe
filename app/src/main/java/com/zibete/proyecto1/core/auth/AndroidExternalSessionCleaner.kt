package com.zibete.proyecto1.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.facebook.login.LoginManager
import com.zibete.proyecto1.BuildConfig
import com.zibete.proyecto1.domain.session.ExternalSessionCleaner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider

class AndroidExternalSessionCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val loginManager: Provider<LoginManager>
) : ExternalSessionCleaner {

    override suspend fun clear() {
        // Local email sessions never initialize or retain external provider credentials.
        if (BuildConfig.IS_LOCAL_BACKEND) return
        loginManager.get().logOut()
        CredentialManager.create(context)
            .clearCredentialState(ClearCredentialStateRequest())
    }
}
