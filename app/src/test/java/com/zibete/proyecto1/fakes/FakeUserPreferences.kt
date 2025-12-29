package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeUserPreferencesState(
    var onboardingDone: Boolean = false,
    var firstLoginDone: Boolean = false,
    var deleteUser: Boolean = false,
)

class FakeUserPreferences(
    private val state: FakeUserPreferencesState = FakeUserPreferencesState(),

    // Flows: los que uses en prod los podés setear desde el test si querés.
    override val groupContextFlow: Flow<GroupContext?> = flowOf(null),
    override val inGroupFlow: Flow<Boolean> = flowOf(false),
    override val applyAgeFilterFlow: Flow<Boolean> = flowOf(false),
    override val applyOnlineFilterFlow: Flow<Boolean> = flowOf(false),
    override val minAgeFlow: Flow<Int> = flowOf(18),
    override val maxAgeFlow: Flow<Int> = flowOf(99),
    override val individualNotificationsFlow: Flow<Boolean> = flowOf(true),
    override val groupNotificationsFlow: Flow<Boolean> = flowOf(true),
    override val filterSwitchFlow: Flow<Boolean> = flowOf(false),
    override val groupNameFlow: Flow<String> = flowOf("")
) : UserPreferencesProvider, UserPreferencesActions {

    // --- Provider ---
    override suspend fun isOnboardingDone(): Boolean = state.onboardingDone
    override suspend fun isFirstLoginDone(): Boolean = state.firstLoginDone
    override suspend fun isDeleteUser(): Boolean = state.deleteUser

    // --- Actions ---
    var setOnboardingDoneCalls = 0

    override suspend fun setOnboardingDone(done: Boolean) {
        state.onboardingDone = done
        setOnboardingDoneCalls++
    }

    override suspend fun setFirstLoginDone(done: Boolean) {
        state.firstLoginDone = done
    }

    override suspend fun setDeleteUser(done: Boolean) {
        state.deleteUser = done
    }

    override suspend fun resetGroupState() {}
    override suspend fun clearAllData() {}
    override suspend fun setApplyAgeFilter(value: Boolean) {}
    override suspend fun setApplyOnlineFilter(value: Boolean) {}
    override suspend fun setMinAge(value: Int) {}
    override suspend fun setMaxAge(value: Int) {}
    override suspend fun setFilterSwitch(value: Boolean) {}
    override suspend fun setGroupNotifications(value: Boolean) {}
    override suspend fun setIndividualNotifications(value: Boolean) {}

    override suspend fun setGroupSession(groupName: String, userName: String, userType: Int) {}
}
