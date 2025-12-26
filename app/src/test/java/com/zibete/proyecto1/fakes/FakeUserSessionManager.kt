package com.zibete.proyecto1.fakes

import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider

class FakeUserSessionProvider(
    override var currentUser: FirebaseUser? = null
) : UserSessionProvider

class FakeUserSessionActions(
    private val intentToReturn: Intent = Intent()
) : UserSessionActions {

    override suspend fun logOutCleanup(): Intent = intentToReturn
}

