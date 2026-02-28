package com.zibete.proyecto1.data.auth

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult

enum class AuthProvider { PASSWORD, GOOGLE, FACEBOOK, OTHER, NONE }

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
    suspend fun updateEmail(newEmail: String): ZibeResult<Unit>
    suspend fun updatePassword(newPassword: String): ZibeResult<Unit>
    suspend fun reauthenticate(credentials: String?): Boolean
}
