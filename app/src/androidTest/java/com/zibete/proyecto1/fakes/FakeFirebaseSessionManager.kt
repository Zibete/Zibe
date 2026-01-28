package com.zibete.proyecto1.fakes

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.FirebaseSessionManager.AuthProvider
import com.zibete.proyecto1.testing.TestScenario
import io.mockk.mockk
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FakeAuthSessionProvider(
    private val scenarioProvider: () -> TestScenario
) : AuthSessionProvider {

    override val currentUser: FirebaseUser?
        get() {
            val uid = scenarioProvider().currentUserUid ?: return null

            return mock<FirebaseUser>().apply {
                doReturn(uid).whenever(this).uid
            }
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
    ): ZibeResult<AuthResult> {
        lastEmail = email
        lastPassword = password
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(mockk(relaxed = true))
        }
    }

    override suspend fun updateEmail(newEmail: String): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun updatePassword(newPassword: String): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun reauthenticate(credentials: String?): Boolean =
        !shouldFail
}
