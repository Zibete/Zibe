package com.zibete.proyecto1.fakes

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.mockk.mockk
import org.mockito.kotlin.doReturn

class FakeUserSessionProvider(
    private val scenarioProvider: () -> TestScenario
) : UserSessionProvider {

    override val currentUser: FirebaseUser?
        get() {
            val uid = scenarioProvider().currentUserUid ?: return null

            return mock<FirebaseUser>().apply {
                doReturn(uid).whenever(this).uid
            }
        }
}

class FakeUserSessionActions(
    private val scenarioProvider: () -> TestScenario
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
            scenarioProvider().currentUserUid = TestData.UID
            ZibeResult.Success(mockk(relaxed = true))
        }
    }
}
