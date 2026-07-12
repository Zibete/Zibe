package com.zibete.proyecto1.data.auth

import android.content.Context
import android.net.Uri
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.AuthCredentialRequest.Facebook
import com.zibete.proyecto1.data.auth.AuthCredentialRequest.Google
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseSessionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firebaseAuth: FirebaseAuth
) : AuthSessionProvider, AuthSessionActions {

    private val firebaseUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override val currentUser: AuthUser?
        get() = firebaseUser?.toAuthUser()

    // ---------------------------------------------------------------------------------------------
    // AUTH API
    // ---------------------------------------------------------------------------------------------

    override suspend fun signInWithEmail(email: String, password: String): ZibeResult<Unit> =
        zibeCatching { firebaseAuth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun createUser(email: String, password: String): ZibeResult<AuthUser> =
        zibeCatching {
            checkNotNull(firebaseAuth.createUserWithEmailAndPassword(email, password).await().user)
                .toAuthUser()
        }

    override suspend fun signInWithCredential(
        credential: AuthCredentialRequest
    ): ZibeResult<Unit> = zibeCatching {
        val firebaseCredential = when (credential) {
            is Google -> GoogleAuthProvider.getCredential(credential.idToken, null)
            is Facebook -> FacebookAuthProvider.getCredential(credential.accessToken)
        }
        firebaseAuth.signInWithCredential(firebaseCredential).await()
    }

    override suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit> =
        zibeCatching { firebaseAuth.sendPasswordResetEmail(email).await() }

    override suspend fun deleteFirebaseUser(): ZibeResult<Unit> =
        zibeCatching { firebaseUser?.delete()?.await() }

    override suspend fun signOutFirebaseUser(): ZibeResult<Unit> =
        zibeCatching { firebaseAuth.signOut() }

    // ---------------------------------------------------------------------------------------------
    // PROFILE (AUTH USER)
    // ---------------------------------------------------------------------------------------------

    override suspend fun updateAuthProfile(userName: String, photoUrl: String?): ZibeResult<Unit> =
        zibeCatching {
            firebaseUser?.updateProfile(
                UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .apply { photoUrl?.let { photoUri = Uri.parse(it) } }
                .build())?.await()
        }

    override suspend fun updateEmail(newEmail: String): ZibeResult<Unit> =
        zibeCatching { firebaseUser?.updateEmail(newEmail)?.await() }

    override suspend fun updatePassword(newPassword: String): ZibeResult<Unit> =
        zibeCatching { firebaseUser?.updatePassword(newPassword)?.await() }

    // ---------------------------------------------------------------------------------------------
    // PROVIDER TYPE
    // ---------------------------------------------------------------------------------------------

    override fun authProvider(): AuthProvider {
        val user = firebaseUser ?: return AuthProvider.NONE
        val providers = user.providerData.map { it.providerId }
        return when {
            "password" in providers -> AuthProvider.PASSWORD
            "google.com" in providers -> AuthProvider.GOOGLE
            "facebook.com" in providers -> AuthProvider.FACEBOOK
            else -> AuthProvider.OTHER
        }
    }

    override fun authProviderLabel(): String? = when (authProvider()) {
        AuthProvider.GOOGLE -> "Google"
        AuthProvider.FACEBOOK -> "Facebook"
        else -> null
    }

    override suspend fun reauthenticate(credentials: String?): Boolean {

        val provider = authProvider()
        val user = firebaseUser ?: return false

        val credential = when (provider) {
            AuthProvider.PASSWORD -> {
                val email = user.email.orEmpty()
                if (email.isBlank() || credentials.isNullOrBlank()) return false
                EmailAuthProvider.getCredential(email, credentials)
            }

            AuthProvider.GOOGLE -> {
                val acct = GoogleSignIn.getLastSignedInAccount(appContext) ?: return false
                val token = acct.idToken ?: return false
                GoogleAuthProvider.getCredential(token, null)
            }

            AuthProvider.FACEBOOK -> {
                val token = AccessToken.getCurrentAccessToken()?.token ?: return false
                FacebookAuthProvider.getCredential(token)
            }

            AuthProvider.OTHER -> return false
            AuthProvider.NONE -> return false
        }

        return runCatching {
            user.reauthenticate(credential).await()
            true
        }.getOrDefault(false)
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        email = email
    )
}
