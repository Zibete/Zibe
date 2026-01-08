package com.zibete.proyecto1.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Keys {
    // migration marker (por si querés usarlo luego)
    val MIGRATED_FROM_SP = booleanPreferencesKey("migratedFromSharedPrefs")

    // user/group
    val IN_GROUP = booleanPreferencesKey("inGroup")
    val USER_NAME_GROUP = stringPreferencesKey("userName")
    val GROUP_NAME = stringPreferencesKey("groupName")
    val USER_TYPE = intPreferencesKey("userType")
    val USER_DATE = stringPreferencesKey("userDate")
    val READ_GROUP_MSG = intPreferencesKey("readGroupMsg")

    // filters
    val FILTER_SWITCH = booleanPreferencesKey("filterPrefs")
    val APPLY_ONLINE_FILTER = booleanPreferencesKey("checkPref")
    val APPLY_AGE_FILTER = booleanPreferencesKey("edadPref")
    val MIN_AGE = intPreferencesKey("desdePref")
    val MAX_AGE = intPreferencesKey("hastaPref")

    // notifications / onboarding
    val INDIVIDUAL_NOTIFICATIONS = booleanPreferencesKey("individualNotifications")
    val GROUP_NOTIFICATIONS = booleanPreferencesKey("groupNotifications")
    val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
    val FIRST_LOGIN_DONE = booleanPreferencesKey("firstLoginDone")
    val EDIT_PROFILE_WELCOME_SHOWN = booleanPreferencesKey("editProfileWelcomeShown")
    val DELETE_USER = booleanPreferencesKey("deleteUser")
    val DELETE_FIREBASE_ACCOUNT = booleanPreferencesKey("deleteFirebaseAccount")
}
