package com.zibete.proyecto1.testing

import com.zibete.proyecto1.testing.TestData.RUNTIME_EXCEPTION
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Perillas del escenario para androidTest.
 * El test setea estos valores ANTES de abrir la pantalla.
 */
data class UnitScenario(
    // --- App flow ---
    var onboardingDone: Boolean = true,
    var firstLoginDone: Boolean = true,
    var deleteUser: Boolean = false,

    // --- Device / permissions ---
    var hasInternet: Boolean = true,
    var hasLocationPermission: Boolean = true,

    // --- Session / user ---
    var currentUserUid: String? = null,
    var accountExists: Boolean = true,
    var hasBirthDate: Boolean = true,

    // --- Preferences ---
    var inGroup: Boolean = false,
    var groupName: String = "",
    var filterSwitch: Boolean = false,
    var applyAgeFilter: Boolean = false,
    var applyOnlineFilter: Boolean = false,
    var minAge: Int = 18,
    var maxAge: Int = 99,
    var individualNotifications: Boolean = true,
    var groupNotifications: Boolean = true,

    // --- Failure control ---
    var shouldFail: Boolean = false,
    val runtimeException: Throwable = RuntimeException(RUNTIME_EXCEPTION)
)

@Singleton
class UnitScenarioStore @Inject constructor() {
    @Volatile var scenario: UnitScenario = UnitScenario()
}
