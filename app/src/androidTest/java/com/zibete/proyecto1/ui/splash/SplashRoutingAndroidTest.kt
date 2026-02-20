package com.zibete.proyecto1.ui.splash

import android.content.Intent
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.ONBOARDING_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.testing.waitText
import com.zibete.proyecto1.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SplashSessionConflictAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {
    val logout = context.getString(R.string.logout)
    val keepHere = context.getString(R.string.session_conflict_keep_here)
    val attentionTitle = context.getString(R.string.attention_title)

    @Before
    fun setup() {
        Intents.init()
        val intent = Intent(context, SplashActivity::class.java).apply {
            putExtra(EXTRA_SESSION_CONFLICT, true)
        }

        launchWithScenario(
            scenario = TestScenario(
                onboardingDone = true,
                currentUserUid = TestData.UID
            ),
            intent = intent
        )
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun smoke_whenSessionConflict_dialogShows() {
        waitText(attentionTitle, composeRule)
    }

    @Test
    fun flow_whenSessionConflict_dialogShows_andLogout_NavigatesToAuth() {
        waitText(attentionTitle, composeRule)

        composeRule.onNodeWithText(logout).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }

    @Test
    fun flow_whenSessionConflict_dialogShows_andKeepHere_NavigatesToMain() {
        waitText(attentionTitle, composeRule)

        composeRule.onNodeWithText(keepHere).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                Intents.intended(hasComponent(MainActivity::class.java.name))
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SplashOnboardingAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {
    val next = context.getString(R.string.onboarding_next)
    val start = context.getString(R.string.onboarding_start)

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

        composeRule.onNodeWithText(context.getString(R.string.onboarding_skip)).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }

    @Test
    fun flow_whenOnboardingNotDone_navigatesToOnboarding_andNext_andNext_andStart_NavigatesToAuth() {
        waitTag(ONBOARDING_SCREEN, composeRule)

        composeRule.onNodeWithText(next).performClick()
        composeRule.onNodeWithText(next).performClick()
        composeRule.onNodeWithText(start).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }
}

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SplashToMainAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {
    @Before
    fun setup() {
        Intents.init()
        try {
            launchWithScenario(
                TestScenario(
                    onboardingDone = true,
                    currentUserUid = TestData.UID
                )
            )
        } catch (t: Throwable) {
            Intents.release()
            throw t
        }
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
    }
}

@RunWith(AndroidJUnit4::class)
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
