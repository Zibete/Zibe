package com.zibete.proyecto1.data.auth

import android.content.Context
import android.net.Uri
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseSessionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firebaseAuth: FirebaseAuth
) : AuthSessionProvider, AuthSessionActions {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    // ---------------------------------------------------------------------------------------------
    // AUTH API
    // ---------------------------------------------------------------------------------------------

    override suspend fun signInWithEmail(email: String, password: String): ZibeResult<AuthResult> =
        zibeCatching { firebaseAuth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun createUser(email: String, password: String): ZibeResult<AuthResult> =
        zibeCatching { firebaseAuth.createUserWithEmailAndPassword(email, password).await() }

    override suspend fun signInWithCredential(credential: AuthCredential): ZibeResult<Unit> =
        zibeCatching { firebaseAuth.signInWithCredential(credential).await() }

    override suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit> =
        zibeCatching { firebaseAuth.sendPasswordResetEmail(email).await() }

    override suspend fun deleteFirebaseUser(): ZibeResult<Unit> =
        zibeCatching { currentUser?.delete()?.await() }

    override suspend fun signOutFirebaseUser(): ZibeResult<Unit> =
        zibeCatching { firebaseAuth.signOut() }

    // ---------------------------------------------------------------------------------------------
    // PROFILE (AUTH USER)
    // ---------------------------------------------------------------------------------------------

    override suspend fun updateAuthProfile(userName: String, photoUrl: String?): ZibeResult<Unit> =
        zibeCatching {
            currentUser?.updateProfile(
                UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .apply { photoUrl?.let { photoUri = Uri.parse(it) } }
                .build())?.await()
        }

    override suspend fun updateEmail(newEmail: String): ZibeResult<Unit> =
        zibeCatching { currentUser?.updateEmail(newEmail)?.await() }

    override suspend fun updatePassword(newPassword: String): ZibeResult<Unit> =
        zibeCatching { currentUser?.updatePassword(newPassword)?.await() }

    // ---------------------------------------------------------------------------------------------
    // PROVIDER TYPE
    // ---------------------------------------------------------------------------------------------

    override fun authProvider(): AuthProvider {
        val user = currentUser ?: return AuthProvider.NONE
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
        val user = currentUser ?: return false

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
}
