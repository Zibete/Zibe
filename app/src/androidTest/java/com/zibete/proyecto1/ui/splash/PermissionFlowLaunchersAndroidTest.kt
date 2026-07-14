package com.zibete.proyecto1.ui.splash

import android.Manifest
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.notifications.NotificationPermissionSnapshot
import com.zibete.proyecto1.notifications.NotificationPermissionStateProvider
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.custompermission.CustomPermissionScreen
import com.zibete.proyecto1.ui.custompermission.PermissionEducationMode
import com.zibete.proyecto1.ui.custompermission.PermissionViewModel
import com.zibete.proyecto1.ui.theme.ZibeTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionFlowLaunchersAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun continueRequestsLocationThenNotificationAndAllowAllowCompletesOnce() {
        val harness = launchHarness(
            mode = PermissionEducationMode.COMBINED,
            hasLocationPermission = false
        )

        clickContinue()
        harness.registry.awaitLaunches(1)
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            harness.registry.launchedInputs
        )

        harness.registry.respond(granted = true)
        harness.registry.awaitLaunches(2)
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            ),
            harness.registry.launchedInputs
        )
        assertEquals(1, harness.notificationProvider.markRequestedCount)

        harness.registry.respond(granted = true)

        composeRule.waitUntil { harness.completedCount.get() == 1 }
        assertEquals(0, harness.locationDeniedCount.get())
        assertEquals(2, harness.registry.launchedInputs.size)
    }

    @Test
    fun notificationDenialStillCompletesAndDoesNotLoop() {
        val harness = launchHarness(
            mode = PermissionEducationMode.COMBINED,
            hasLocationPermission = false
        )

        clickContinue()
        harness.registry.awaitLaunches(1)
        harness.registry.respond(granted = true)
        harness.registry.awaitLaunches(2)
        harness.registry.respond(granted = false)

        composeRule.waitUntil { harness.completedCount.get() == 1 }
        composeRule.waitForIdle()
        assertEquals(0, harness.locationDeniedCount.get())
        assertEquals(2, harness.registry.launchedInputs.size)
        assertTrue(harness.notificationProvider.snapshot().wasRequested)
    }

    @Test
    fun locationDenialStopsBeforeNotificationsAndRoutesThroughMandatoryDenial() {
        val harness = launchHarness(
            mode = PermissionEducationMode.COMBINED,
            hasLocationPermission = false
        )

        clickContinue()
        harness.registry.awaitLaunches(1)
        harness.registry.respond(granted = false)

        composeRule.onNodeWithText(resourceString(R.string.permission_denied_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(resourceString(R.string.action_accept)).performClick()

        composeRule.waitUntil { harness.locationDeniedCount.get() == 1 }
        assertEquals(0, harness.completedCount.get())
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            harness.registry.launchedInputs
        )
        assertEquals(0, harness.notificationProvider.markRequestedCount)
    }

    @Test
    fun notificationOnlySkipsLocationAndCompletesAfterOptionalDenial() {
        val harness = launchHarness(
            mode = PermissionEducationMode.NOTIFICATION_ONLY,
            hasLocationPermission = true
        )

        clickContinue()
        harness.registry.awaitLaunches(1)
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            harness.registry.launchedInputs
        )

        harness.registry.respond(granted = false)

        composeRule.waitUntil { harness.completedCount.get() == 1 }
        assertEquals(0, harness.locationDeniedCount.get())
        assertEquals(1, harness.notificationProvider.markRequestedCount)
    }

    @Test
    fun inFlightRequestIsNotDuplicatedAfterScreenRecreation() {
        val harness = launchHarness(
            mode = PermissionEducationMode.NOTIFICATION_ONLY,
            hasLocationPermission = true
        )

        clickContinue()
        harness.registry.awaitLaunches(1)
        harness.recreateScreen()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PERMISSION_SCREEN).assertIsDisplayed()
        assertEquals(1, harness.registry.launchedInputs.size)
        assertEquals(1, harness.notificationProvider.markRequestedCount)
    }

    private fun launchHarness(
        mode: PermissionEducationMode,
        hasLocationPermission: Boolean
    ): PermissionFlowHarness {
        val scenario = TestScenario(hasLocationPermission = hasLocationPermission)
        val notificationProvider = RecordingNotificationPermissionStateProvider()
        val viewModel = PermissionViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider { scenario },
            notificationPermissionStateProvider = notificationProvider
        )
        val registry = RecordingActivityResultRegistry()
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        val completedCount = AtomicInteger()
        val locationDeniedCount = AtomicInteger()
        val generation = mutableIntStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                key(generation.intValue) {
                    ZibeTheme {
                        CustomPermissionScreen(
                            mode = mode,
                            onPermissionFlowCompleted = { completedCount.incrementAndGet() },
                            onLocationDenied = { locationDeniedCount.incrementAndGet() },
                            permissionViewModel = viewModel
                        )
                    }
                }
            }
        }

        return PermissionFlowHarness(
            notificationProvider = notificationProvider,
            registry = registry,
            completedCount = completedCount,
            locationDeniedCount = locationDeniedCount,
            recreateScreen = {
                composeRule.runOnIdle { generation.intValue += 1 }
            }
        )
    }

    private fun clickContinue() {
        composeRule.onNodeWithText(resourceString(R.string.action_continue)).performClick()
    }

    private fun resourceString(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun RecordingActivityResultRegistry.awaitLaunches(expected: Int) {
        composeRule.waitUntil { launchedInputs.size == expected }
    }
}

private data class PermissionFlowHarness(
    val notificationProvider: RecordingNotificationPermissionStateProvider,
    val registry: RecordingActivityResultRegistry,
    val completedCount: AtomicInteger,
    val locationDeniedCount: AtomicInteger,
    val recreateScreen: () -> Unit
)

private class RecordingActivityResultRegistry : ActivityResultRegistry() {
    private val pendingRequestCodes = ArrayDeque<Int>()
    val launchedInputs = mutableListOf<String>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    ) {
        launchedInputs += input as String
        pendingRequestCodes += requestCode
    }

    fun respond(granted: Boolean) {
        check(pendingRequestCodes.isNotEmpty()) { "No permission request is pending" }
        dispatchResult(pendingRequestCodes.removeFirst(), granted)
    }
}

private class RecordingNotificationPermissionStateProvider :
    NotificationPermissionStateProvider {
    private var status = NotificationPermissionSnapshot(
        requiresRuntimePermission = true,
        isRuntimePermissionGranted = false,
        wasRequested = false,
        areAppNotificationsEnabled = false,
        isMessageChannelEnabled = true
    )

    var markRequestedCount = 0
        private set

    override fun snapshot(): NotificationPermissionSnapshot = status

    override fun markRequested() {
        markRequestedCount += 1
        status = status.copy(wasRequested = true)
    }
}
