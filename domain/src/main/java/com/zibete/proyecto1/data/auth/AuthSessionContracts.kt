package com.zibete.proyecto1.data.auth

import com.zibete.proyecto1.core.utils.ZibeResult

enum class AuthProvider { PASSWORD, GOOGLE, FACEBOOK, OTHER, NONE }

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val photoUrl: String?,
    val email: String?
)

sealed interface AuthCredentialRequest {
    data class Google(val idToken: String) : AuthCredentialRequest
    data class Facebook(val accessToken: String) : AuthCredentialRequest
}

interface AuthSessionProvider {
    val currentUser: AuthUser?
    fun authProvider(): AuthProvider
    fun authProviderLabel(): String?
}

interface AuthSessionActions {
    suspend fun signInWithEmail(email: String, password: String): ZibeResult<Unit>
    suspend fun signInWithCredential(credential: AuthCredentialRequest): ZibeResult<Unit>
    suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit>
    suspend fun deleteFirebaseUser(): ZibeResult<Unit>
    suspend fun signOutFirebaseUser(): ZibeResult<Unit>
    suspend fun createUser(email: String, password: String): ZibeResult<AuthUser>
    suspend fun updateAuthProfile(userName: String, photoUrl: String?): ZibeResult<Unit>
    suspend fun updateEmail(newEmail: String): ZibeResult<Unit>
    suspend fun updatePassword(newPassword: String): ZibeResult<Unit>
    suspend fun reauthenticate(credentials: String?): Boolean
}
