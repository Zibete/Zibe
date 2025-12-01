package com.zibete.proyecto1.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar las SharedPreferences de la aplicación (filtros, estado de grupo, notificaciones).
 * Utiliza @Inject constructor para que Hilt maneje su ciclo de vida como Singleton.
 */
@Singleton // Hilt asegura que solo exista una instancia
class UserPreferencesRepository @Inject constructor(
    // Hilt inyecta el Contexto de la Aplicación (seguro)
    @ApplicationContext context: Context
) {
    // Inicializamos con ApplicationContext para evitar Memory Leaks
    private val userPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
    private val appPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
    private val filterPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("filterPrefs", Context.MODE_PRIVATE)

    // Nota: El antiguo 'companion object' y 'getInstance' fueron eliminados
    // porque Hilt (@Singleton) maneja esa lógica automáticamente.

    // ==========================================
    // SECCIÓN 1: DATOS DE USUARIO Y GRUPO
    // ==========================================

    var inGroup: Boolean
        get() = userPrefs.getBoolean("inGroup", false)
        set(value) = userPrefs.edit { putBoolean("inGroup", value) }

    var userNameGroup: String
        get() = userPrefs.getString("userName", "") ?: ""
        set(value) = userPrefs.edit { putString("userName", value) }

    var groupName: String
        get() = userPrefs.getString("groupName", "") ?: ""
        set(value) = userPrefs.edit { putString("groupName", value) }

    var unknownUserId: String
        get() = userPrefs.getString("unknownUserId", "") ?: ""
        set(value) = userPrefs.edit { putString("unknownUserId", value) }

    var userType: Int
        get() = userPrefs.getInt("userType", 2)
        set(value) = userPrefs.edit { putInt("userType", value) }

    var userDate: String
        get() = userPrefs.getString("userDate", "") ?: ""
        set(value) = userPrefs.edit { putString("userDate", value) }

    var readGroupMsg: Int
        get() = userPrefs.getInt("readGroupMsg", 0)
        set(value) = userPrefs.edit { putInt("readGroupMsg", value) }

    // ==========================================
    // SECCIÓN 2: FILTROS
    // ==========================================

    var filterSwitch: Boolean
        get() = filterPrefs.getBoolean("filterPrefs", false)
        set(value) = filterPrefs.edit { putBoolean("filterPrefs", value) }

    var checkPref: Boolean
        get() = filterPrefs.getBoolean("checkPref", false)
        set(value) = filterPrefs.edit { putBoolean("checkPref", value) }

    var edadPref: Boolean
        get() = filterPrefs.getBoolean("edadPref", false)
        set(value) = filterPrefs.edit { putBoolean("edadPref", value) }

    var desdePref: Int
        get() = filterPrefs.getInt("desdePref", 0)
        set(value) = filterPrefs.edit { putInt("desdePref", value) }

    var hastaPref: Int
        get() = filterPrefs.getInt("hastaPref", 0)
        set(value) = filterPrefs.edit { putInt("hastaPref", value) }

    // ==========================================
    // SECCIÓN 3: NOTIFICACIONES - ONBOARDING
    // ==========================================

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

    var deleteUser: Boolean
        get() = appPrefs.getBoolean("deleteUser", false)
        set(value) = appPrefs.edit { putBoolean("deleteUser", value) }

    var deleteFirebaseAccount: Boolean
        get() = appPrefs.getBoolean("deleteFirebaseAccount", false)
        set(value) = appPrefs.edit { putBoolean("deleteFirebaseAccount", value) }

    // ==========================================
    // SECCIÓN 4: ACCIONES EN BLOQUE (Optimizado)
    // ==========================================

    /**
     * Guarda la info de sesión del usuario de una sola vez.
     */
    fun saveUserSession(
        inGroup: Boolean,
        userName: String,
        groupName: String,
        userType: Int,
        readGroupMsg: Int,
        userDate: String
    ) {
        userPrefs.edit {
            putBoolean("inGroup", inGroup)
            putString("userName", userName)
            putString("groupName", groupName)
            putInt("userType", userType)
            putInt("readGroupMsg", readGroupMsg)
            putString("userDate", userDate)
        }
    }

    /**
     * Limpia la sesión y resetea filtros (llamado en Logout).
     */
    fun clearAllData() {
        userPrefs.edit {
            // Reseteo Usuario
            putBoolean("inGroup", false)
            putString("groupName", "")
            putString("userName", "")
            putInt("userType", 2)
            putInt("readGroupMsg", 0)
            putString("userDate", "")

            // Reseteo Filtros (Manteniendo las notificaciones)
            putBoolean("filterPrefs", false)
            putBoolean("checkPref", false)
            putBoolean("edadPref", false)
            putInt("desdePref", 0)
            putInt("hastaPref", 0)
        }
    }

    /**
     * Resetea solo el estado de la sesión de grupo (llamado al Salir del Grupo).
     * [CRÍTICO]: Se aseguró que solo use el editor de prefs para evitar recursividad
     * o llamadas a un Singleton externo que ya no existe.
     */
    fun resetGroupState() {
        // Limpiamos todos los campos relacionados con el estado de grupo de forma atómica.
        userPrefs.edit {
            putBoolean("inGroup", false)
            putString("userName", "")
            putString("groupName", "")
            putInt("userType", 2)
            putInt("readGroupMsg", 0)
            putString("userDate", "")
        }
    }
}