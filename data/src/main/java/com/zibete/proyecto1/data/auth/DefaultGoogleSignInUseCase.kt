package com.zibete.proyecto1.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import javax.inject.Inject
import javax.inject.Named

interface GoogleSignInUseCase {
    suspend operator fun invoke(activity: Activity): ZibeResult<String>
}

class DefaultGoogleSignInUseCase @Inject constructor(
    @Named("web_client_id") private val serverClientId: String
) : GoogleSignInUseCase {
    override suspend operator fun invoke(
        activity: Activity
    ): ZibeResult<String> = zibeCatching {

        val cm = CredentialManager.create(activity)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(true)
                    .build()
            )
            .build()

        val result = cm.getCredential(
            request = request,
            context = activity
        )

        GoogleIdTokenCredential
            .createFrom(result.credential.data)
            .idToken
    }
}


