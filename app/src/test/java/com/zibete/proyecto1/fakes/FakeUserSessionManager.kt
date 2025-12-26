package com.zibete.proyecto1.fakes

import android.content.Intent
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider

class FakeUserSessionProvider(
    override var currentUser: FirebaseUser? = null
) : UserSessionProvider

class FakeUserSessionActions(
    private val intentToReturn: Intent = Intent()
) : UserSessionActions {

    var signInShouldFail: Boolean = false
    var signInFailure: Throwable = RuntimeException("fake auth failure")


    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun logOutCleanup(): Intent = intentToReturn

    override suspend fun signInWithEmail(email: String, password: String) {
        lastEmail = email
        lastPassword = password
        if (signInShouldFail) throw signInFailure
    }

    override suspend fun signInWithCredential(credential: AuthCredential) {
        if (signInShouldFail) throw signInFailure
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        if (signInShouldFail) throw signInFailure
    }

    override suspend fun deleteFirebaseUser() {
        if (signInShouldFail) throw signInFailure
    }
}
