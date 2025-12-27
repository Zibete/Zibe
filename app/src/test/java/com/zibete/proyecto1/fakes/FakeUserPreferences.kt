package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider

class FakeUserPreferencesState(
    var onboardingDone: Boolean = false,
    var firstLoginDone: Boolean = false,
    var deleteUser: Boolean = false
)

class FakeUserPreferencesProvider(
    private val state: FakeUserPreferencesState
) : UserPreferencesProvider {

    override suspend fun isOnboardingDone(): Boolean = state.onboardingDone
    override suspend fun isFirstLoginDone(): Boolean = state.firstLoginDone
    override suspend fun isDeleteUser(): Boolean = state.deleteUser
}

class FakeUserPreferencesActions(
    private val state: FakeUserPreferencesState,
    var setOnboardingDoneCalls: Int = 0
) : UserPreferencesActions {

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
}