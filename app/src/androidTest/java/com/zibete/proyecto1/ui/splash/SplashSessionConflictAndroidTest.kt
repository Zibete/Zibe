package com.zibete.proyecto1.ui.splash

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.TestScenarioHolder
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.ui.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_LOGOUT
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_TITLE
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SplashSessionConflictAndroidTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<SplashActivity>()

    @Before
    fun setup() {
        // 1) escenario: usuario presente
        TestScenarioHolder.scenario = TestScenario(
            currentUserUid = "test_uid"
        )

        // 2) Hilt
        hiltRule.inject()

        // 3) forzamos extra en el Intent actual
        composeRule.activity.intent.putExtra(EXTRA_SESSION_CONFLICT, true)

        // 4) recreamos Activity para que lea el extra en onCreate
        composeRule.activityRule.scenario.recreate()

        composeRule.waitForIdle()
    }

    @Test
    fun whenSessionConflict_dialogShows_andDismissNavigatesToAuth() {
        // 1) aparece el dialog
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithText(SESSION_CONFLICT_TITLE).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        composeRule.onNodeWithText(SESSION_CONFLICT_TITLE).assertExists()

        // 2) click logout
        composeRule.onNodeWithText(SESSION_CONFLICT_LOGOUT).performClick()

        // 3) navega a Auth
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(AUTH_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AUTH_SCREEN).assertExists()
    }
}
