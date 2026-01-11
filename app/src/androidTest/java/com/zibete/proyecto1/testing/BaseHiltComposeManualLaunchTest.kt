package com.zibete.proyecto1.testing

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule
import javax.inject.Inject

abstract class BaseHiltComposeManualLaunchTest<A : ComponentActivity>(
    private val activityClass: Class<A>
) {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // No ata Compose a ninguna Activity, solo lo usamos para waitUntil si hace falta
    @get:Rule(order = 1)
    val composeRule: ComposeTestRule = createEmptyComposeRule()

    @Inject lateinit var store: TestScenarioStore

    protected fun launchWithScenario(
        scenario: TestScenario,
        intent: Intent? = null
    ): ActivityScenario<A> {
        // Orden correcto: primero inject (para tener store), después seteo escenario, después launch
        hiltRule.inject()
        store.scenario = scenario

        val context = ApplicationProvider.getApplicationContext<Context>()
        val i = intent ?: Intent(context, activityClass)
        return ActivityScenario.launch(i)
    }
}
