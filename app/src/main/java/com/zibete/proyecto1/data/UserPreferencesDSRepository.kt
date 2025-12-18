package com.zibete.proyecto1.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class GroupContext(
    val inGroup: Boolean,
    val groupName: String,
    val userName: String,
    val userType: Int,
    val userDate: String
)

@Singleton
class UserPreferencesDSRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ---------------------------------------------------------------------------------------------
    // GROUP (source of truth)
    // ---------------------------------------------------------------------------------------------

    /** Contexto reactivo del grupo (si no está en grupo -> null) */
    val groupContextFlow: Flow<GroupContext?> =
        dataStore.data
            .map { prefs ->
                val inGroup = prefs[Keys.IN_GROUP] ?: false
                val groupName = prefs[Keys.GROUP_NAME].orEmpty()

                if (!inGroup || groupName.isBlank()) return@map null

                GroupContext(
                    inGroup = true,
                    groupName = groupName,
                    userName = prefs[Keys.USER_NAME_GROUP].orEmpty(),
                    userType = prefs[Keys.USER_TYPE] ?: PUBLIC_USER,
                    userDate = prefs[Keys.USER_DATE].orEmpty()
                )
            }
            .distinctUntilChanged()

    /** Flag simple (reactivo) por si alguna pantalla lo necesita */
    val inGroupFlow: Flow<Boolean> =
        dataStore.data
            .map { it[Keys.IN_GROUP] ?: false }
            .distinctUntilChanged()

    /** groupName reactivo (útil para toolbar o labels sin armar GroupContext) */
    val groupNameFlow: Flow<String> =
        dataStore.data
            .map { it[Keys.GROUP_NAME].orEmpty() }
            .distinctUntilChanged()

    suspend fun setGroupSession(
        groupName: String,
        userName: String,
        userType: Int,
        userDate: String
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.IN_GROUP] = true
            prefs[Keys.GROUP_NAME] = groupName
            prefs[Keys.USER_NAME_GROUP] = userName
            prefs[Keys.USER_TYPE] = userType
            prefs[Keys.USER_DATE] = userDate
        }
    }

    suspend fun resetGroupState() {
        dataStore.edit { prefs ->
            prefs[Keys.IN_GROUP] = false
            prefs[Keys.GROUP_NAME] = ""
            prefs[Keys.USER_NAME_GROUP] = ""
            prefs[Keys.USER_TYPE] = PUBLIC_USER
            prefs[Keys.USER_DATE] = ""
            prefs[Keys.READ_GROUP_MSG] = 0
        }
    }

    val readGroupMsgFlow: Flow<Int> =
        dataStore.data
            .map { it[Keys.READ_GROUP_MSG] ?: 0 }
            .distinctUntilChanged()

    suspend fun setReadGroupMsg(value: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.READ_GROUP_MSG] = value
        }
    }

    // ---------------------------------------------------------------------------------------------
    // FILTERS (reactivo)
    // ---------------------------------------------------------------------------------------------

    val filterSwitchFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.FILTER_SWITCH] ?: false }.distinctUntilChanged()

    suspend fun setFilterSwitch(value: Boolean) {
        dataStore.edit { it[Keys.FILTER_SWITCH] = value }
    }

    val applyOnlineFilterFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.APPLY_ONLINE_FILTER] ?: false }.distinctUntilChanged()

    suspend fun setApplyOnlineFilter(value: Boolean) {
        dataStore.edit { it[Keys.APPLY_ONLINE_FILTER] = value }
    }

    val applyAgeFilterFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.APPLY_AGE_FILTER] ?: false }.distinctUntilChanged()

    suspend fun setApplyAgeFilter(value: Boolean) {
        dataStore.edit { it[Keys.APPLY_AGE_FILTER] = value }
    }

    val minAgeFlow: Flow<Int> =
        dataStore.data.map { it[Keys.MIN_AGE] ?: 0 }.distinctUntilChanged()

    suspend fun setMinAge(value: Int) {
        dataStore.edit { it[Keys.MIN_AGE] = value }
    }

    val maxAgeFlow: Flow<Int> =
        dataStore.data.map { it[Keys.MAX_AGE] ?: 0 }.distinctUntilChanged()

    suspend fun setMaxAge(value: Int) {
        dataStore.edit { it[Keys.MAX_AGE] = value }
    }

    // ---------------------------------------------------------------------------------------------
    // NOTIFICATIONS / ONBOARDING (reactivo + setters)
    // ---------------------------------------------------------------------------------------------

    val individualNotificationsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.INDIVIDUAL_NOTIFICATIONS] ?: true }.distinctUntilChanged()

    suspend fun setIndividualNotifications(value: Boolean) {
        dataStore.edit { it[Keys.INDIVIDUAL_NOTIFICATIONS] = value }
    }

    val groupNotificationsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.GROUP_NOTIFICATIONS] ?: true }.distinctUntilChanged()

    suspend fun setGroupNotifications(value: Boolean) {
        dataStore.edit { it[Keys.GROUP_NOTIFICATIONS] = value }
    }

    val onboardingDoneFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }.distinctUntilChanged()

    suspend fun setOnboardingDone(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = value }
    }

    val firstLoginDoneFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.FIRST_LOGIN_DONE] ?: false }.distinctUntilChanged()

    suspend fun setFirstLoginDone(value: Boolean) {
        dataStore.edit { it[Keys.FIRST_LOGIN_DONE] = value }
    }

    val deleteUserFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.DELETE_USER] ?: false }.distinctUntilChanged()

    suspend fun setDeleteUser(value: Boolean) {
        dataStore.edit { it[Keys.DELETE_USER] = value }
    }

    val deleteFirebaseAccountFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.DELETE_FIREBASE_ACCOUNT] ?: false }.distinctUntilChanged()

    suspend fun setDeleteFirebaseAccount(value: Boolean) {
        dataStore.edit { it[Keys.DELETE_FIREBASE_ACCOUNT] = value }
    }

    // ---------------------------------------------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------------------------------------------

    /** Equivalente a tu clearAllData() actual */
    suspend fun clearAllData() {
        resetGroupState()
        dataStore.edit { prefs ->
            prefs[Keys.FILTER_SWITCH] = false
            prefs[Keys.APPLY_ONLINE_FILTER] = false
            prefs[Keys.APPLY_AGE_FILTER] = false
            prefs[Keys.MIN_AGE] = 0
            prefs[Keys.MAX_AGE] = 0

            prefs[Keys.ONBOARDING_DONE] = false
            prefs[Keys.FIRST_LOGIN_DONE] = false
            prefs[Keys.DELETE_USER] = false
            prefs[Keys.DELETE_FIREBASE_ACCOUNT] = false
        }
    }
}
