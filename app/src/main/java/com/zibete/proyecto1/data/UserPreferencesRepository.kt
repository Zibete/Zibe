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
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("FilterUsers", Context.MODE_PRIVATE)

    // Nota: El antiguo 'companion object' y 'getInstance' fueron eliminados
    // porque Hilt (@Singleton) maneja esa lógica automáticamente.

    // ==========================================
    // SECCIÓN 1: DATOS DE USUARIO Y GRUPO
    // ==========================================

    var inGroup: Boolean
        get() = prefs.getBoolean("inGroup", false)
        set(value) = prefs.edit { putBoolean("inGroup", value) }

    var userName: String
        get() = prefs.getString("userName", "") ?: ""
        set(value) = prefs.edit { putString("userName", value) }

    var groupName: String
        get() = prefs.getString("groupName", "") ?: ""
        set(value) = prefs.edit { putString("groupName", value) }

    var userType: Int
        get() = prefs.getInt("userType", 2)
        set(value) = prefs.edit { putInt("userType", value) }

    var userDate: String
        get() = prefs.getString("userDate", "") ?: ""
        set(value) = prefs.edit { putString("userDate", value) }

    var readGroupMsg: Int
        get() = prefs.getInt("readGroupMsg", 0)
        set(value) = prefs.edit { putInt("readGroupMsg", value) }

    // ==========================================
    // SECCIÓN 2: FILTROS
    // ==========================================

    var filterPrefs: Boolean
        get() = prefs.getBoolean("filterPrefs", false)
        set(value) = prefs.edit { putBoolean("filterPrefs", value) }

    var checkPref: Boolean
        get() = prefs.getBoolean("checkPref", false)
        set(value) = prefs.edit { putBoolean("checkPref", value) }

    var edadPref: Boolean
        get() = prefs.getBoolean("edadPref", false)
        set(value) = prefs.edit { putBoolean("edadPref", value) }

    var desdePref: Int
        get() = prefs.getInt("desdePref", 0)
        set(value) = prefs.edit { putInt("desdePref", value) }

    var hastaPref: Int
        get() = prefs.getInt("hastaPref", 0)
        set(value) = prefs.edit { putInt("hastaPref", value) }

    // ==========================================
    // SECCIÓN 3: NOTIFICACIONES
    // ==========================================

    var individualNotifications: Boolean
        get() = prefs.getBoolean("individualNotifications", true)
        set(value) = prefs.edit { putBoolean("individualNotifications", value) }

    var groupNotifications: Boolean
        get() = prefs.getBoolean("groupNotifications", true)
        set(value) = prefs.edit { putBoolean("groupNotifications", value) }

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
        prefs.edit {
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
        prefs.edit {
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
        prefs.edit {
            putBoolean("inGroup", false)
            putString("userName", "")
            putString("groupName", "")
            putInt("userType", 2)
            putInt("readGroupMsg", 0)
            putString("userDate", "")
        }
    }
}