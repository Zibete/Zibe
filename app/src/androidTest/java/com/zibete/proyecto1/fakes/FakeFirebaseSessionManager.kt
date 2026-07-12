package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.auth.AuthCredentialRequest
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.AuthProvider
import com.zibete.proyecto1.data.auth.AuthUser
import com.zibete.proyecto1.testing.TestScenario

class FakeAuthSessionProvider(
    private val scenarioProvider: () -> TestScenario
) : AuthSessionProvider {

    override val currentUser: AuthUser?
        get() {
            val uid = scenarioProvider().currentUserUid ?: return null

            return AuthUser(uid, null, null, null)
        }

    override fun authProvider(): AuthProvider {
        return AuthProvider.PASSWORD
    }

    override fun authProviderLabel(): String? {
        return "Password"
    }
}

class FakeAuthSessionActions(
    private val scenarioProvider: () -> TestScenario
) : AuthSessionActions {

    var lastEmail: String? = null
    var lastPassword: String? = null
    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): ZibeResult<Unit> {
        lastEmail = email
        lastPassword = password
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
    }

    override suspend fun signInWithCredential(
        credential: AuthCredentialRequest
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

    override suspend fun signOutFirebaseUser(): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun updateAuthProfile(
        userName: String,
        photoUrl: String?
    ): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun createUser(
        email: String,
        password: String
    ): ZibeResult<AuthUser> {
        lastEmail = email
        lastPassword = password
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(AuthUser("uid", null, null, email))
        }
    }

    override suspend fun updateEmail(newEmail: String): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun updatePassword(newPassword: String): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun reauthenticate(credentials: String?): Boolean =
        !shouldFail
}
