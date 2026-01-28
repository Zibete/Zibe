package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.testing.TestScenario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeUserPreferencesProvider(
    private val scenarioProvider: () -> TestScenario
) : UserPreferencesProvider {

    private val mutex = Mutex()

    // --- Provider ---
    override suspend fun isOnboardingDone(): Boolean =
        mutex.withLock { scenarioProvider().onboardingDone }

    override suspend fun isFirstLoginDone(): Boolean =
        mutex.withLock { scenarioProvider().firstLoginDone }

    override suspend fun isEditProfileWelcomeShown(): Boolean = false

    override val groupContextFlow: Flow<GroupContext?> = flowOf(null)
    override val inGroupFlow: Flow<Boolean> = flowOf(false)
    override val applyAgeFilterFlow: Flow<Boolean> = flowOf(false)
    override val applyOnlineFilterFlow: Flow<Boolean> = flowOf(false)
    override val minAgeFlow: Flow<Int> = flowOf(18)
    override val maxAgeFlow: Flow<Int> = flowOf(99)
    override val individualNotificationsFlow: Flow<Boolean> = flowOf(true)
    override val groupNotificationsFlow: Flow<Boolean> = flowOf(true)
    override val filterSwitchFlow: Flow<Boolean> = flowOf(false)
    override val groupNameFlow: Flow<String> = flowOf("")
}

class FakeUserPreferencesActions(
    private val scenarioProvider: () -> TestScenario
) : UserPreferencesActions {

    private val mutex = Mutex()

    // --- Actions ---
    override suspend fun setOnboardingDone(done: Boolean) {
        mutex.withLock { scenarioProvider().onboardingDone = done }
    }

    override suspend fun setFirstLoginDone(done: Boolean) {
        mutex.withLock { scenarioProvider().firstLoginDone = done }
    }

    override suspend fun setEditProfileWelcomeShown(done: Boolean) { /*...*/
    }

    override suspend fun resetGroupState() {}
    override suspend fun clearSessionData() {}
    override suspend fun setApplyAgeFilter(value: Boolean) {}
    override suspend fun setApplyOnlineFilter(value: Boolean) {}
    override suspend fun setMinAge(value: Int) {}
    override suspend fun setMaxAge(value: Int) {}
    override suspend fun setFilterSwitch(value: Boolean) {}
    override suspend fun setGroupNotifications(value: Boolean) {}
    override suspend fun setIndividualNotifications(value: Boolean) {}

    override suspend fun setGroupSession(groupName: String, userName: String, userType: Int) {}
}

