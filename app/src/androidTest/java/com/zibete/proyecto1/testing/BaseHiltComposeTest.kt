package com.zibete.proyecto1.testing

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule
import javax.inject.Inject

abstract class BaseHiltComposeTest<A : ComponentActivity>(
    private val activityClass: Class<A>
) {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Compose rule "normal" (requiere ActivityScenarioRule<Class<A>>)
    @get:Rule(order = 1)
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A> =
        createAndroidComposeRule(activityClass)

    @Inject lateinit var store: TestScenarioStore

    /**
     * Setea scenario (inyectado por Hilt) ANTES de que uses la UI.
     * OJO: como createAndroidComposeRule auto-lanza la Activity, esta función
     * se usa para tests donde el scenario NO necesita estar seteado antes del primer frame.
     */
    protected fun setScenario(scenario: TestScenario) {
        hiltRule.inject()
        store.scenario = scenario
    }

    /**
     * Versión “correcta” cuando el scenario DEBE estar seteado antes de crear la Activity:
     * - No uses createAndroidComposeRule(activityClass) en ese test.
     * - En ese test usás createEmptyComposeRule() + ActivityScenario.launch().
     *
     * (Te dejo el ejemplo más abajo)
     */
    protected fun launchActivityManually(intent: Intent? = null): ActivityScenario<A> {
        return ActivityScenario.launch(intent ?: Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), activityClass))
    }
}
