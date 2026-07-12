package com.zibete.proyecto1.ui.main

import android.Manifest
import android.view.View
import androidx.navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zibete.proyecto1.R
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MainNavigationAndroidTest :
    BaseHiltComposeManualLaunchTest<MainActivity>(MainActivity::class.java) {
    private lateinit var mainScenario: ActivityScenario<MainActivity>

    @Before
    fun launchMain() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        mainScenario = launchWithScenario(
            TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                firstLoginDone = true,
                hasLocationPermission = true
            )
        )
    }

    @Test
    fun mainStartsInChatsWithExpectedRootOrderAndNoDrawer() {
        waitTag(ACCOUNT_AVATAR_TAG, composeRule)

        launchWithCurrentActivity { activity ->
            val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
            assertEquals(R.id.navBottomChat, bottomNav.selectedItemId)
            assertEquals(
                listOf(
                    R.id.navBottomUsers,
                    R.id.navBottomChat,
                    R.id.navBottomGroups,
                    R.id.navBottomFavorites
                ),
                (0 until bottomNav.menu.size()).map { bottomNav.menu.getItem(it).itemId }
            )
            assertNull(activity.findViewById<View>(
                activity.resources.getIdentifier("drawerLayout", "id", activity.packageName)
            ))
            assertNull(activity.findViewById<MaterialToolbar>(R.id.materialToolbar).navigationIcon)
        }
    }

    @Test
    fun accountAvatarOpensSheetAndEditProfileNavigation() {
        waitTag(ACCOUNT_AVATAR_TAG, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_AVATAR_TAG).performClick()
        waitTag(ACCOUNT_SHEET_TAG, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_EDIT_PROFILE_TAG).performClick()

        composeRule.waitUntil(10_000) {
            currentDestinationId() == R.id.editProfileFragment
        }
        composeRule.onNodeWithTag(ACCOUNT_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun accountSettingsActionNavigatesWithoutReopeningSheet() {
        waitTag(ACCOUNT_AVATAR_TAG, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_AVATAR_TAG).performClick()
        waitTag(ACCOUNT_SHEET_TAG, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_SETTINGS_TAG).performClick()

        composeRule.waitUntil(10_000) {
            currentDestinationId() == R.id.settingsFragment
        }
        composeRule.onNodeWithTag(ACCOUNT_SHEET_TAG).assertDoesNotExist()
    }

    private fun currentDestinationId(): Int? {
        var destinationId: Int? = null
        launchWithCurrentActivity { activity ->
            destinationId = activity.findNavController(R.id.nav_host_fragment)
                .currentDestination?.id
        }
        return destinationId
    }

    private fun launchWithCurrentActivity(block: (MainActivity) -> Unit) {
        mainScenario.onActivity(block)
    }
}
