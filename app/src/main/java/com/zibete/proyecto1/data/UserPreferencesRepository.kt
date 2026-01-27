package com.zibete.proyecto1.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesProvider, UserPreferencesActions {

    // ---------------------------------------------------------------------------------------------
    // GROUP (source of truth)
    // ---------------------------------------------------------------------------------------------

    /** Contexto reactivo del grupo (si no está en grupo -> null) */
    override val groupContextFlow: Flow<GroupContext?> =
        dataStore.data
            .map { prefs ->
                val inGroup = prefs[Keys.IN_GROUP] ?: false
                val groupName = prefs[Keys.GROUP_NAME].orEmpty()

                if (!inGroup || groupName.isBlank()) return@map null

                GroupContext(
                    inGroup = true,
                    groupName = groupName,
                    userName = prefs[Keys.USER_NAME_GROUP].orEmpty(),
                    userType = prefs[Keys.USER_TYPE] ?: PUBLIC_USER
                )
            }
            .distinctUntilChanged()

    /** Flag simple (reactivo) por si alguna pantalla lo necesita */
    override val inGroupFlow: Flow<Boolean> =
        dataStore.data
            .map { it[Keys.IN_GROUP] ?: false }
            .distinctUntilChanged()

    /** groupName reactivo (útil para toolbar o labels sin armar GroupContext) */
    override val groupNameFlow: Flow<String> =
        dataStore.data
            .map { it[Keys.GROUP_NAME].orEmpty() }
            .distinctUntilChanged()

    override suspend fun setGroupSession(
        groupName: String,
        userName: String,
        userType: Int
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.IN_GROUP] = true
            prefs[Keys.GROUP_NAME] = groupName
            prefs[Keys.USER_NAME_GROUP] = userName
            prefs[Keys.USER_TYPE] = userType
        }
    }

    override suspend fun resetGroupState() {
        dataStore.edit { prefs ->
            prefs[Keys.IN_GROUP] = false
            prefs[Keys.GROUP_NAME] = ""
            prefs[Keys.USER_NAME_GROUP] = ""
            prefs[Keys.USER_TYPE] = PUBLIC_USER
        }
    }

    // ---------------------------------------------------------------------------------------------
    // FILTERS (reactivo)
    // ---------------------------------------------------------------------------------------------

    override val filterSwitchFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.FILTER_SWITCH] ?: false }.distinctUntilChanged()

    override suspend fun setFilterSwitch(value: Boolean) {
        dataStore.edit { it[Keys.FILTER_SWITCH] = value }
    }

    override val applyOnlineFilterFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.APPLY_ONLINE_FILTER] ?: false }.distinctUntilChanged()

    override suspend fun setApplyOnlineFilter(value: Boolean) {
        dataStore.edit { it[Keys.APPLY_ONLINE_FILTER] = value }
    }

    override val applyAgeFilterFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.APPLY_AGE_FILTER] ?: false }.distinctUntilChanged()

    override suspend fun setApplyAgeFilter(value: Boolean) {
        dataStore.edit { it[Keys.APPLY_AGE_FILTER] = value }
    }

    override val minAgeFlow: Flow<Int> =
        dataStore.data.map { it[Keys.MIN_AGE] ?: 0 }.distinctUntilChanged()

    override suspend fun setMinAge(value: Int) {
        dataStore.edit { it[Keys.MIN_AGE] = value }
    }

    override val maxAgeFlow: Flow<Int> =
        dataStore.data.map { it[Keys.MAX_AGE] ?: 0 }.distinctUntilChanged()

    override suspend fun setMaxAge(value: Int) {
        dataStore.edit { it[Keys.MAX_AGE] = value }
    }

    // ---------------------------------------------------------------------------------------------
    // NOTIFICATIONS / ONBOARDING (reactivo + setters)
    // ---------------------------------------------------------------------------------------------

    override val individualNotificationsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.INDIVIDUAL_NOTIFICATIONS] ?: true }.distinctUntilChanged()

    override suspend fun setIndividualNotifications(value: Boolean) {
        dataStore.edit { it[Keys.INDIVIDUAL_NOTIFICATIONS] = value }
    }

    override val groupNotificationsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.GROUP_NOTIFICATIONS] ?: true }.distinctUntilChanged()

    override suspend fun setGroupNotifications(value: Boolean) {
        dataStore.edit { it[Keys.GROUP_NOTIFICATIONS] = value }
    }

    override suspend fun isOnboardingDone(): Boolean =
        dataStore.data.first()[Keys.ONBOARDING_DONE] ?: false

    override suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    override suspend fun isFirstLoginDone(): Boolean =
        dataStore.data.first()[Keys.FIRST_LOGIN_DONE] ?: false

    override suspend fun setFirstLoginDone(done: Boolean) {
        dataStore.edit { it[Keys.FIRST_LOGIN_DONE] = done }
    }

    override suspend fun isEditProfileWelcomeShown(): Boolean =
        dataStore.data.first()[Keys.EDIT_PROFILE_WELCOME_SHOWN] ?: false

    override suspend fun setEditProfileWelcomeShown(done: Boolean) {
        dataStore.edit { it[Keys.EDIT_PROFILE_WELCOME_SHOWN] = done }
    }

    // ---------------------------------------------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------------------------------------------

    override suspend fun clearSessionData() {
        resetGroupState()
        dataStore.edit { prefs ->
            prefs[Keys.FILTER_SWITCH] = false
            prefs[Keys.APPLY_ONLINE_FILTER] = false
            prefs[Keys.APPLY_AGE_FILTER] = false
            prefs[Keys.MIN_AGE] = 0
            prefs[Keys.MAX_AGE] = 0

            prefs[Keys.FIRST_LOGIN_DONE] = false
        }
    }
}
