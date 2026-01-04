package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.testing.TestScenario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeUserPreferencesProvider(
    private val scenarioProvider: () -> TestScenario
) : UserPreferencesProvider {
    // --- Provider ---
    override suspend fun isOnboardingDone(): Boolean = scenarioProvider().onboardingDone
    override suspend fun isFirstLoginDone(): Boolean = scenarioProvider().firstLoginDone
    override suspend fun isDeleteUser(): Boolean = scenarioProvider().deleteUser

    override val groupContextFlow: Flow<GroupContext?> = flow {
        emit(null) // si querés modelarlo en scenario, agregás un field GroupContext?
    }

    override val inGroupFlow: Flow<Boolean> = flow { emit(scenarioProvider().inGroup) }
    override val applyAgeFilterFlow: Flow<Boolean> = flow { emit(scenarioProvider().applyAgeFilter) }
    override val applyOnlineFilterFlow: Flow<Boolean> = flow { emit(scenarioProvider().applyOnlineFilter) }
    override val minAgeFlow: Flow<Int> = flow { emit(scenarioProvider().minAge) }
    override val maxAgeFlow: Flow<Int> = flow { emit(scenarioProvider().maxAge) }
    override val individualNotificationsFlow: Flow<Boolean> = flow { emit(scenarioProvider().individualNotifications) }
    override val groupNotificationsFlow: Flow<Boolean> = flow { emit(scenarioProvider().groupNotifications) }
    override val filterSwitchFlow: Flow<Boolean> = flow { emit(scenarioProvider().filterSwitch) }
    override val groupNameFlow: Flow<String> = flow { emit(scenarioProvider().groupName) }
}

class FakeUserPreferencesActions(
    private val scenarioProvider: () -> TestScenario
) : UserPreferencesActions {

    override suspend fun setOnboardingDone(done: Boolean) { scenarioProvider().onboardingDone = done }
    override suspend fun setFirstLoginDone(done: Boolean) { scenarioProvider().firstLoginDone = done }
    override suspend fun setDeleteUser(done: Boolean) { scenarioProvider().deleteUser = done }

    override suspend fun resetGroupState() {
        scenarioProvider().inGroup = false
        scenarioProvider().groupName = ""
    }

    override suspend fun clearAllData() {
        val s = scenarioProvider()
        s.onboardingDone = false
        s.firstLoginDone = false
        s.deleteUser = false
        s.inGroup = false
        s.groupName = ""
        s.filterSwitch = false
        s.applyAgeFilter = false
        s.applyOnlineFilter = false
        s.minAge = 18
        s.maxAge = 99
        s.individualNotifications = true
        s.groupNotifications = true
    }

    override suspend fun setApplyAgeFilter(value: Boolean) { scenarioProvider().applyAgeFilter = value }
    override suspend fun setApplyOnlineFilter(value: Boolean) { scenarioProvider().applyOnlineFilter = value }
    override suspend fun setMinAge(value: Int) { scenarioProvider().minAge = value }
    override suspend fun setMaxAge(value: Int) { scenarioProvider().maxAge = value }
    override suspend fun setFilterSwitch(value: Boolean) { scenarioProvider().filterSwitch = value }
    override suspend fun setGroupNotifications(value: Boolean) { scenarioProvider().groupNotifications = value }
    override suspend fun setIndividualNotifications(value: Boolean) { scenarioProvider().individualNotifications = value }

    override suspend fun setGroupSession(groupName: String, userName: String, userType: Int) {
        scenarioProvider().inGroup = true
        scenarioProvider().groupName = groupName
    }
}