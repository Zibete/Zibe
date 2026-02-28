package com.zibete.proyecto1.data

import kotlinx.coroutines.flow.Flow

data class GroupContext(
    val inGroup: Boolean,
    val groupName: String,
    val userName: String,
    val userType: Int
)

interface UserPreferencesProvider {
    suspend fun isOnboardingDone(): Boolean
    suspend fun isFirstLoginDone(): Boolean
    suspend fun isEditProfileWelcomeShown(): Boolean
    val groupContextFlow: Flow<GroupContext?>
    val inGroupFlow: Flow<Boolean>
    val applyAgeFilterFlow: Flow<Boolean>
    val applyOnlineFilterFlow: Flow<Boolean>
    val minAgeFlow: Flow<Int>
    val maxAgeFlow: Flow<Int>
    val individualNotificationsFlow: Flow<Boolean>
    val groupNotificationsFlow: Flow<Boolean>
    val filterSwitchFlow: Flow<Boolean>
    val groupNameFlow: Flow<String>
}

interface UserPreferencesActions {
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setFirstLoginDone(done: Boolean)
    suspend fun setEditProfileWelcomeShown(done: Boolean)
    suspend fun resetGroupState()
    suspend fun clearSessionData()
    suspend fun setApplyAgeFilter(value: Boolean)
    suspend fun setApplyOnlineFilter(value: Boolean)
    suspend fun setMinAge(value: Int)
    suspend fun setMaxAge(value: Int)
    suspend fun setFilterSwitch(value: Boolean)
    suspend fun setGroupNotifications(value: Boolean)
    suspend fun setIndividualNotifications(value: Boolean)
    suspend fun setGroupSession(groupName: String, userName: String, userType: Int)
}
