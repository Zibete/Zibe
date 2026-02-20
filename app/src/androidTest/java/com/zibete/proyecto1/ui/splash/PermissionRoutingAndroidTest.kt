package com.zibete.proyecto1.ui.splash

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestPermissionConfig
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.testing.waitText
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PermissionRoutingAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {
    val actionStart = context.getString(R.string.action_start)
    val actionAccept = context.getString(R.string.action_accept)
    val actionCancel = context.getString(R.string.action_cancel)
    val rationaleTitle = context.getString(R.string.permission_rationale_title)
    val deniedTitle = context.getString(R.string.permission_denied_title)

    @Before
    fun resetPermissionConfig() {
        TestPermissionConfig.shouldShowRationale = false
        TestPermissionConfig.grantResult = true
    }

    @Test
    fun flow_startClicked_withoutRationale_grantedTrue_navigatesToSplash() {
        TestPermissionConfig.shouldShowRationale = false
        TestPermissionConfig.grantResult = true

        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)

        composeRule.onNodeWithText(actionStart).performClick()

        waitTag(SPLASH_SCREEN, composeRule)
    }

    @Test
    fun flow_startClicked_rationaleAccept_grantedTrue_navigatesToSplash() {
        TestPermissionConfig.shouldShowRationale = true
        TestPermissionConfig.grantResult = true

        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)
        composeRule.onNodeWithText(actionStart).performClick()

        if (composeRule.onAllNodesWithText(rationaleTitle)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        ) {
            composeRule.onNodeWithText(actionAccept).performClick()
        }

        waitTag(SPLASH_SCREEN, composeRule)
    }

    @Test
    fun flow_startClicked_rationaleCancel_staysOnPermission() {
        TestPermissionConfig.shouldShowRationale = true
        TestPermissionConfig.grantResult = false

        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)
        composeRule.onNodeWithText(actionStart).performClick()

        waitText(rationaleTitle, composeRule)
        composeRule.onNodeWithText(actionCancel).performClick()

        composeRule.onNodeWithText(rationaleTitle).assertDoesNotExist()
        composeRule.onNodeWithText(actionStart).assertExists()
    }

    @Test
    fun flow_startClicked_withoutRationale_grantedFalse_deniedOk_navigatesToAuth() {
        TestPermissionConfig.shouldShowRationale = false
        TestPermissionConfig.grantResult = false

        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)
        composeRule.onNodeWithText(context.getString(R.string.action_start)).performClick()

        waitText(deniedTitle, composeRule)
        composeRule.onNodeWithText(actionAccept).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }

    @Test
    fun flow_startClicked_withRationaleAccept_grantedFalse_deniedOk_navigatesToAuth() {
        TestPermissionConfig.shouldShowRationale = true
        TestPermissionConfig.grantResult = false

        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)
        composeRule.onNodeWithText(context.getString(R.string.action_start)).performClick()

        waitText(rationaleTitle, composeRule)
        composeRule.onNodeWithText(actionAccept).performClick()

        waitText(deniedTitle, composeRule)
        composeRule.onNodeWithText(actionAccept).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }
}
