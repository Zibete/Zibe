package com.zibete.proyecto1.ui.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.ui.custompermission.PermissionEducationContent
import com.zibete.proyecto1.ui.custompermission.PermissionEducationMode
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PermissionRoutingAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Test
    fun missingLocationShowsCombinedEducation() {
        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = false,
                notificationRuntimeRequired = true,
                notificationPermissionGranted = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)
        composeRule.onNodeWithText(context.getString(R.string.permission_location_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.permission_notifications_title))
            .assertIsDisplayed()
    }

    @Test
    fun existingUserWithPendingNotificationShowsNotificationOnlyEducation() {
        launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                hasInternet = true,
                hasLocationPermission = true,
                notificationRuntimeRequired = true,
                notificationPermissionGranted = false,
                notificationWasRequested = false,
                systemNotificationsEnabled = false
            )
        )

        waitTag(PERMISSION_SCREEN, composeRule)
        composeRule.onNodeWithText(context.getString(R.string.permission_notifications_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.permission_location_title))
            .assertDoesNotExist()
    }
}

@RunWith(AndroidJUnit4::class)
class PermissionEducationContentAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun combinedEducationShowsContractCopyAndContinueAction() {
        val clicked = AtomicBoolean(false)
        composeRule.setContent {
            ZibeTheme {
                PermissionEducationContent(
                    mode = PermissionEducationMode.COMBINED,
                    requestInFlight = false,
                    onContinue = { clicked.set(true) }
                )
            }
        }

        composeRule.onNodeWithText("Conectate y no te pierdas nada").assertIsDisplayed()
        composeRule.onNodeWithText("Ubicación").assertIsDisplayed()
        composeRule.onNodeWithText("Notificaciones").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.runOnIdle { assertTrue(clicked.get()) }
    }

    @Test
    fun notificationOnlyEducationDoesNotRepeatLocationSection() {
        composeRule.setContent {
            ZibeTheme {
                PermissionEducationContent(
                    mode = PermissionEducationMode.NOTIFICATION_ONLY,
                    requestInFlight = false,
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithText("Notificaciones").assertIsDisplayed()
        composeRule.onNodeWithText("Ubicación").assertDoesNotExist()
    }
}
