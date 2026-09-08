package com.zibete.proyecto1.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesProvider, UserPreferencesActions {

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
