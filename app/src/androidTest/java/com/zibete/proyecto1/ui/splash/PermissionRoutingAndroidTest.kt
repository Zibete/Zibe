package com.zibete.proyecto1.ui.splash

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zibete.proyecto1.core.constants.BUTTON_START
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.core.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.core.constants.DIALOG_CANCEL
import com.zibete.proyecto1.core.constants.DIALOG_OK
import com.zibete.proyecto1.core.constants.PERMISSION_DENIED_TITLE
import com.zibete.proyecto1.core.constants.PERMISSION_RATIONALE_TITLE
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestPermissionConfig
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.testing.waitText
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class PermissionRoutingAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

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

        composeRule.onNodeWithText(BUTTON_START).performClick()

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
        composeRule.onNodeWithText(BUTTON_START).performClick()

        waitText(PERMISSION_RATIONALE_TITLE, composeRule)
        composeRule.onNodeWithText(DIALOG_ACCEPT).performClick()

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
        composeRule.onNodeWithText(BUTTON_START).performClick()

        waitText(PERMISSION_RATIONALE_TITLE, composeRule)
        composeRule.onNodeWithText(DIALOG_CANCEL).performClick()

        composeRule.onNodeWithText(PERMISSION_RATIONALE_TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(BUTTON_START).assertExists()
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
        composeRule.onNodeWithText(BUTTON_START).performClick()

        waitText(PERMISSION_DENIED_TITLE, composeRule)
        composeRule.onNodeWithText(DIALOG_OK).performClick()

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
        composeRule.onNodeWithText(BUTTON_START).performClick()

        waitText(PERMISSION_RATIONALE_TITLE, composeRule)
        composeRule.onNodeWithText(DIALOG_ACCEPT).performClick()

        waitText(PERMISSION_DENIED_TITLE, composeRule)
        composeRule.onNodeWithText(DIALOG_OK).performClick()

        waitTag(AUTH_SCREEN, composeRule)
    }

}
