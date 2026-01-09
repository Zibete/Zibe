package com.zibete.proyecto1.testing

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

abstract class BaseHiltComposeManualLaunchTest<A : ComponentActivity>(
    private val activityClass: Class<A>
) {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule: ComposeTestRule = createEmptyComposeRule()

    val context: Context get() = ApplicationProvider.getApplicationContext()

    @Inject
    lateinit var store: TestScenarioStore

    @Before
    fun baseSetup() {
        hiltRule.inject()
    }

    private var scenarioInstance: ActivityScenario<A>? = null

    @After
    fun baseTearDown() {
        scenarioInstance?.close()
    }

    protected fun launchWithScenario(
        scenario: TestScenario,
        intent: Intent? = null
    ): ActivityScenario<A> {
        store.scenario = scenario

        val i = intent ?: Intent(context, activityClass)
        return ActivityScenario.launch<A>(i).also {
            scenarioInstance = it
        }
    }
}
