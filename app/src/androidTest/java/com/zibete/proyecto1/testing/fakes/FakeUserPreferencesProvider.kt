package com.zibete.proyecto1.testing.fakes

import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.testing.TestScenario

class FakeUserPreferencesProvider(
    private val scenario: TestScenario
) : UserPreferencesProvider {

    override suspend fun isOnboardingDone(): Boolean = scenario.onboardingDone
    override suspend fun isFirstLoginDone(): Boolean = true
    override suspend fun isDeleteUser(): Boolean = false
}
