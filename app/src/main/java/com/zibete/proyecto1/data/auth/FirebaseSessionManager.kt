package com.zibete.proyecto1.data.auth

import androidx.core.net.toUri
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.FirebaseSessionManager.AuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface AuthSessionProvider {
    val currentUser: FirebaseUser?
    fun authProvider(): AuthProvider
    fun authProviderLabel(): String?
}

interface AuthSessionActions {
    suspend fun signInWithEmail(email: String, password: String): ZibeResult<AuthResult>
    suspend fun signInWithCredential(credential: AuthCredential): ZibeResult<Unit>
    suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit>
    suspend fun deleteFirebaseUser(): ZibeResult<Unit>
    suspend fun signOutFirebaseUser(): ZibeResult<Unit>
    suspend fun createUser(email: String, password: String): ZibeResult<AuthResult>
    suspend fun updateAuthProfile(userName: String, photoUrl: String?): ZibeResult<Unit>
}

class FirebaseSessionManager @Inject constructor(
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
                .apply { photoUrl?.let { photoUri = it.toUri() } }
                .build())?.await()
        }

    // ---------------------------------------------------------------------------------------------
    // PROVIDER TYPE
    // ---------------------------------------------------------------------------------------------

    enum class AuthProvider { PASSWORD, GOOGLE, FACEBOOK, OTHER, NONE }

    override fun authProvider(): AuthProvider {
        val user = firebaseAuth.currentUser ?: return AuthProvider.NONE
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
}