package com.zibete.proyecto1.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar las SharedPreferences de la aplicación (filtros, estado de grupo, notificaciones).
 * Utiliza @Inject constructor para que Hilt maneje su ciclo de vida como Singleton.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val userPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
    private val appPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
    private val filterPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("filterPrefs", Context.MODE_PRIVATE)

    // DATOS DE USUARIO Y GRUPO
    var inGroup: Boolean
        get() = userPrefs.getBoolean("inGroup", false)
        set(value) = userPrefs.edit { putBoolean("inGroup", value) }

    var userNameGroup: String
        get() = userPrefs.getString("userName", "") ?: ""
        set(value) = userPrefs.edit { putString("userName", value) }

    var groupName: String
        get() = userPrefs.getString("groupName", "") ?: ""
        set(value) = userPrefs.edit { putString("groupName", value) }

    var userType: Int
        get() = userPrefs.getInt("userType", PUBLIC_USER)
        set(value) = userPrefs.edit { putInt("userType", value) }

    var userDate: String
        get() = userPrefs.getString("userDate", "") ?: ""
        set(value) = userPrefs.edit { putString("userDate", value) }

    var readGroupMsg: Int
        get() = userPrefs.getInt("readGroupMsg", 0)
        set(value) = userPrefs.edit { putInt("readGroupMsg", value) }

    // FILTROS
    var filterSwitch: Boolean
        get() = filterPrefs.getBoolean("filterPrefs", false)
        set(value) = filterPrefs.edit { putBoolean("filterPrefs", value) }

    var applyOnlineFilter: Boolean
        get() = filterPrefs.getBoolean("checkPref", false)
        set(value) = filterPrefs.edit { putBoolean("checkPref", value) }

    var applyAgeFilter: Boolean
        get() = filterPrefs.getBoolean("edadPref", false)
        set(value) = filterPrefs.edit { putBoolean("edadPref", value) }

    var minAgePref: Int
        get() = filterPrefs.getInt("desdePref", 0)
        set(value) = filterPrefs.edit { putInt("desdePref", value) }

    var maxAgePref: Int
        get() = filterPrefs.getInt("hastaPref", 0)
        set(value) = filterPrefs.edit { putInt("hastaPref", value) }

    // NOTIFICACIONES - ONBOARDING
    var individualNotifications: Boolean
        get() = appPrefs.getBoolean("individualNotifications", true)
        set(value) = appPrefs.edit { putBoolean("individualNotifications", value) }

    var groupNotifications: Boolean
        get() = appPrefs.getBoolean("groupNotifications", true)
        set(value) = appPrefs.edit { putBoolean("groupNotifications", value) }

    var onboardingDone: Boolean
        get() = appPrefs.getBoolean("onboardingDone", false)
        set(value) = appPrefs.edit { putBoolean("onboardingDone", value) }

    var firstLoginDone: Boolean
        get() = appPrefs.getBoolean("firstLoginDone", false)
        set(value) = appPrefs.edit { putBoolean("firstLoginDone", value) }

    var onboardingProfileDone: Boolean
        get() = appPrefs.getBoolean("onboardingProfileDone", false)
        set(value) = appPrefs.edit { putBoolean("onboardingProfileDone", value) }

//    var firstLoginDone: Boolean
//        get() = appPrefs.getBoolean("firstLoginDone", false)
//        set(value) = appPrefs.edit { putBoolean("firstLoginDone", value) }

    var deleteUser: Boolean
        get() = appPrefs.getBoolean("deleteUser", false)
        set(value) = appPrefs.edit { putBoolean("deleteUser", value) }

    var deleteFirebaseAccount: Boolean
        get() = appPrefs.getBoolean("deleteFirebaseAccount", false)
        set(value) = appPrefs.edit { putBoolean("deleteFirebaseAccount", value) }

    fun clearAllData() {

        resetGroupState()

        userPrefs.edit {
            putBoolean("filterPrefs", false)
            putBoolean("checkPref", false)
            putBoolean("edadPref", false)
            putInt("desdePref", 0)
            putInt("hastaPref", 0)
        }
    }

    fun resetGroupState() {
        userPrefs.edit {
            putBoolean("inGroup", false)
            putString("userName", "")
            putString("groupName", "")
            putInt("userType", PUBLIC_USER)
            putInt("readGroupMsg", 0)
            putString("userDate", "")
        }
    }
}