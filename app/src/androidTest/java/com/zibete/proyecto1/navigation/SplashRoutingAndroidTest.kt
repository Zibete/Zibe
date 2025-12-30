package com.zibete.proyecto1.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.testing.waitText
import com.zibete.proyecto1.ui.constants.BUTTON_NEXT
import com.zibete.proyecto1.ui.constants.BUTTON_SKIP
import com.zibete.proyecto1.ui.constants.BUTTON_START
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.ui.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.ui.constants.Constants.UiTags.ONBOARDING_SCREEN
import com.zibete.proyecto1.ui.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_KEEP_HERE
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_LOGOUT
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_TITLE
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class SplashSessionConflictAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, SplashActivity::class.java).apply {
            putExtra(EXTRA_SESSION_CONFLICT, true)
        }

        launchWithScenario(
            scenario = TestScenario(currentUserUid = "test_uid"),
            intent = intent
        )
    }

    @Test
    fun smoke_whenSessionConflict_dialogShows() {
        waitText(SESSION_CONFLICT_TITLE, composeRule)
    }

    @Test
    fun flow_whenSessionConflict_dialogShows_andLogout_NavigatesToAuth() {
        // 1) aparece el dialog
        waitText(SESSION_CONFLICT_TITLE, composeRule)

        // 2) click logout
        composeRule.onNodeWithText(SESSION_CONFLICT_LOGOUT).performClick()

        // 3) navega a Auth
        waitTag(AUTH_SCREEN, composeRule)
    }

    @Test
    fun flow_whenSessionConflict_dialogShows_andKeepHere_NavigatesToSplash() {
        // 1) aparece el dialog
        waitText(SESSION_CONFLICT_TITLE, composeRule)
        // 2) click logout
        composeRule.onNodeWithText(SESSION_CONFLICT_KEEP_HERE).performClick()

        // 3) navega a Splash
        waitTag(SPLASH_SCREEN, composeRule)
    }
}

@HiltAndroidTest
class SplashOnboardingAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        launchWithScenario(
            TestScenario(
                onboardingDone = false
            )
        )
    }

    @Test
    fun smoke_whenOnboardingNotDone_navigatesToOnboarding() {
        waitTag(ONBOARDING_SCREEN, composeRule)
    }

    @Test
    fun flow_whenOnboardingNotDone_navigatesToOnboarding_andSkip_NavigatesToAuth() {
        waitTag(ONBOARDING_SCREEN, composeRule)

        composeRule.onNodeWithText(BUTTON_SKIP).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }

    @Test
    fun flow_whenOnboardingNotDone_navigatesToOnboarding_andNext_andNext_andStart_NavigatesToAuth() {
        waitTag(ONBOARDING_SCREEN, composeRule)

        composeRule.onNodeWithText(BUTTON_NEXT).performClick()
        composeRule.onNodeWithText(BUTTON_NEXT).performClick()
        composeRule.onNodeWithText(BUTTON_START).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }
}

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
    fun whenAllChecksOk_andUserPresent_startsMainActivity() {

        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                Intents.intended(hasComponent(MainActivity::class.java.name))
                true
            } catch (_: AssertionError) {
                false
            }
        }

        Intents.intended(hasComponent(MainActivity::class.java.name))
    }
}

@HiltAndroidTest
class SplashToAuthAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        launchWithScenario(
            TestScenario(
                currentUserUid = null
            )
        )
    }

    @Test
    fun smoke_whenUserIsNull_navigatesToAuth() {
        waitTag(AUTH_SCREEN, composeRule)
    }
}