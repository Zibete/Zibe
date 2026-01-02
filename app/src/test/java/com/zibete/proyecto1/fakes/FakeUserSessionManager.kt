package com.zibete.proyecto1.fakes

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider

class FakeUserSessionProvider(
    override var currentUser: FirebaseUser? = null
) : UserSessionProvider

class FakeUserSessionActions(
) : UserSessionActions {

    var signInShouldFail: Boolean = false
    var resetShouldFail: Boolean = false
    var resetPasswordCalled = false
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

    override suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit> {
        lastEmail = email
        resetPasswordCalled = true
        if (resetShouldFail) throw resetFailure

        return ZibeResult.Success()
    }

    override suspend fun deleteFirebaseUser() {
        if (signInShouldFail) throw signInFailure
    }
}
