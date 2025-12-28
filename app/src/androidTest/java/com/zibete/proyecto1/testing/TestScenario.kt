package com.zibete.proyecto1.testing

import com.google.firebase.auth.FirebaseUser

/**
 * Perillas del escenario para androidTest.
 * El test setea estos valores antes de abrir la pantalla.
 */
data class TestScenario(
    var onboardingDone: Boolean = true,
    var hasInternet: Boolean = true,
    var hasLocationPermission: Boolean = true,
    var currentUser: FirebaseUser? = null
)