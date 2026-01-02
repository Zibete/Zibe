package com.zibete.proyecto1.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        activity: Activity
    ): String {
        val cm = CredentialManager.create(activity)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(true) // Login automático si solo hay una cuenta
                    .build()
            )
            .build()

        // Debe ejecutarse en Main
        val result = cm.getCredential(
            request = request,
            context = activity
        )

        return zibeCatching {
            GoogleIdTokenCredential
                .createFrom(result.credential.data)
                .idToken
        }.getOrThrow()
    }
}


