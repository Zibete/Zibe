package com.zibete.proyecto1.testing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Perillas del escenario para androidTest.
 * El test setea estos valores ANTES de abrir la pantalla.
 */
data class UnitScenario(
    var onboardingDone: Boolean = true,
    var firstLoginDone: Boolean = true,
    var deleteUser: Boolean = false,
    var hasInternet: Boolean = true,
    var hasLocationPermission: Boolean = true,
    var currentUserUid: String? = null
)

@Singleton
class UnitScenarioStore @Inject constructor() {
    @Volatile var scenario: UnitScenario = UnitScenario()
}
object UnitScenarioHolder {
    @Volatile var scenario: UnitScenario = UnitScenario()
}
