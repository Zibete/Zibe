package com.zibete.proyecto1.ui.splash

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class SplashToMainAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        Intents.init()

        launchWithScenario(
            TestScenario(
                currentUserUid = "test_uid"
            )
        )
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun launch_startsMainActivity_whenAllChecksOk_andUserPresent() {

        // Espera hasta 10s a que se dispare el intent
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                Intents.intended(hasComponent(MainActivity::class.java.name))
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Asert final (si no pasó el waitUntil, esto falla y te deja el error claro)
        Intents.intended(hasComponent(MainActivity::class.java.name))
    }

}
