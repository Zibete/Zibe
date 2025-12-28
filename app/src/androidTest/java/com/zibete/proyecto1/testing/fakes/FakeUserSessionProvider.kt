package com.zibete.proyecto1.testing.fakes

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.testing.TestScenario

class FakeUserSessionProvider(
    private val scenario: TestScenario
) : UserSessionProvider {

    override val currentUser: FirebaseUser?
        get() = scenario.currentUser
}
