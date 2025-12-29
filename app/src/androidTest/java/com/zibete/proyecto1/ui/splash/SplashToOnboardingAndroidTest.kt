package com.zibete.proyecto1.ui.splash

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.constants.Constants.UiTags.ONBOARDING_SCREEN
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class SplashToOnboardingAndroidTest :
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
    fun launch_navigatesToOnboarding_whenOnboardingNotDone() {
        composeRule.waitUntil(6_000) {
            composeRule.onAllNodesWithTag(ONBOARDING_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ONBOARDING_SCREEN).assertExists()
    }
}
