package com.zibete.proyecto1.fakes

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.testing.UnitScenario
import io.mockk.mockk

class FakeUserSessionProvider(
    override var currentUser: FirebaseUser? = null
) : UserSessionProvider

class FakeUserSessionActions(
    private val scenarioProvider: () -> UnitScenario
) : UserSessionActions {

    var lastEmail: String? = null
    var lastPassword: String? = null

    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    override suspend fun logOutCleanup() = Unit

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): ZibeResult<AuthResult> {
        lastEmail = email
        lastPassword = password
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(mockk(relaxed = true))
        }
    }

    override suspend fun signInWithCredential(
        credential: AuthCredential
    ): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun sendPasswordResetEmail(
        email: String
    ): ZibeResult<Unit> {
        lastEmail = email
        return if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)
    }

    override suspend fun deleteFirebaseUser(): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun updateAuthProfile(
        userName: String,
        photoUrl: String?
    ): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun createUser(
        email: String,
        password: String
    ): ZibeResult<AuthResult> {
        lastEmail = email
        lastPassword = password
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(mockk(relaxed = true))
        }
    }
}
