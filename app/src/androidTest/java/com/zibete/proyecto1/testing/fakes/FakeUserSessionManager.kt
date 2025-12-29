package com.zibete.proyecto1.testing.fakes

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.testing.TestScenario
import io.mockk.every
import io.mockk.mockk

class FakeUserSessionProvider(
    private val scenarioProvider: () -> TestScenario
) : UserSessionProvider {

    override val currentUser: FirebaseUser?
        get() {
            val uid = scenarioProvider().currentUserUid ?: return null

            val user = mockk<FirebaseUser>(relaxed = true)
            every { user.uid } returns uid
            return user
        }

    val isLoggedIn: Boolean
        get() = scenarioProvider().currentUserUid != null
}


class FakeUserSessionActions(
) : UserSessionActions {

    var signInShouldFail: Boolean = false
    var resetShouldFail: Boolean = false
    var signInFailure: Throwable = RuntimeException("fake auth failure")
    var resetFailure: Throwable = RuntimeException("fake reset failure")
    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun logOutCleanup() {}

    override suspend fun signInWithEmail(email: String, password: String) {
        lastEmail = email
        lastPassword = password
        if (signInShouldFail) throw signInFailure
    }

    override suspend fun signInWithCredential(credential: AuthCredential) {
        if (signInShouldFail) throw signInFailure
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        if (resetShouldFail) throw resetFailure
    }

    override suspend fun deleteFirebaseUser() {
        if (signInShouldFail) throw signInFailure
    }
}
