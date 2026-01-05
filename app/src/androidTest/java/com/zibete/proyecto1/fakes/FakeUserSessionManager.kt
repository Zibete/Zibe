package com.zibete.proyecto1.fakes

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
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
            val uid = scenarioProvider()?.currentUserUid ?: return null

            return mock<FirebaseUser>().apply {
                doReturn(uid).whenever(this).uid
            }
        }


    val isLoggedIn: Boolean
        get() = scenarioProvider().currentUserUid != null
}


class FakeUserSessionActions(
) : UserSessionActions {

    var signInShouldFail = false
    var signInFailure: Throwable = IllegalStateException("Sign in failed")

    var resetShouldFail = false
    var resetFailure: Throwable = IllegalStateException("Reset failed")

    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun logOutCleanup() {}

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): ZibeResult<AuthResult> {

        lastEmail = email
        lastPassword = password

        return if (signInShouldFail) {
            ZibeResult.Failure(signInFailure)
        } else {
            ZibeResult.Success(mockAuthResult())
        }
    }

    override suspend fun signInWithCredential(
        credential: AuthCredential
    ): ZibeResult<Unit> =
        if (signInShouldFail) {
            ZibeResult.Failure(signInFailure)
        } else {
            ZibeResult.Success(Unit)
        }


    override suspend fun sendPasswordResetEmail(
        email: String
    ): ZibeResult<Unit> =
        if (resetShouldFail) {
            ZibeResult.Failure(resetFailure)
        } else {
            ZibeResult.Success(Unit)
        }


    override suspend fun deleteFirebaseUser(): ZibeResult<Unit> =
        if (signInShouldFail) {
            ZibeResult.Failure(signInFailure)
        } else {
            ZibeResult.Success(Unit)
        }


    override suspend fun updateAuthProfile(
        userName: String,
        photoUrl: String?
    ): ZibeResult<Unit> =
        ZibeResult.Success(Unit)


    override suspend fun createUser(
        email: String,
        password: String
    ): ZibeResult<AuthResult> =
        if (signInShouldFail) {
            ZibeResult.Failure(signInFailure)
        } else {
            ZibeResult.Success( mockAuthResult() )
        }

    // helper solo para tests
    private fun mockAuthResult(): AuthResult {
        return mockk(relaxed = true)
    }
}
