package com.zibete.proyecto1.ui.custompermission

import androidx.lifecycle.SavedStateHandle
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.notifications.NotificationPermissionSnapshot
import com.zibete.proyecto1.notifications.NotificationPermissionStateProvider
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `location is requested before notifications`() = runTest {
        val scenario = TestScenario(hasLocationPermission = false)
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val vm = buildVm(scenario, provider)
        val first = async { awaitEvent(vm) }
        runCurrent()

        vm.onContinueClicked(shouldShowLocationRationale = false)

        assertEquals(
            PermissionUiEvent.RequestPermission(PermissionRequest.LOCATION),
            first.await()
        )
        assertEquals(0, provider.markRequestedCount)
    }

    @Test
    fun `location grant continues with notification request on Android 13 plus`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val vm = buildVm(TestScenario(hasLocationPermission = false), provider)
        vm.onContinueClicked(false)
        val notificationRequest = async { awaitEvent(vm) }
        runCurrent()

        vm.onLocationResult(granted = true)

        assertEquals(
            PermissionUiEvent.RequestPermission(PermissionRequest.NOTIFICATIONS),
            notificationRequest.await()
        )
        assertEquals(1, provider.markRequestedCount)
    }

    @Test
    fun `location denial never requests notifications`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val vm = buildVm(TestScenario(hasLocationPermission = false), provider)
        vm.onContinueClicked(false)

        vm.onLocationResult(granted = false)

        assertTrue(vm.uiState.value.showLocationDeniedDialog)
        assertEquals(0, provider.markRequestedCount)
        assertNull(vm.uiState.value.requestInFlight)
    }

    @Test
    fun `pre Android 13 completes after location without notification request`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(preAndroid13Status())
        val vm = buildVm(TestScenario(hasLocationPermission = true), provider)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onContinueClicked(false)

        assertEquals(PermissionUiEvent.Completed, event.await())
        assertEquals(0, provider.markRequestedCount)
    }

    @Test
    fun `granted notification permission completes without request`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(grantedNotificationStatus())
        val vm = buildVm(TestScenario(hasLocationPermission = true), provider)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onContinueClicked(false)

        assertEquals(PermissionUiEvent.Completed, event.await())
    }

    @Test
    fun `notification grant completes permission flow`() = runTest {
        assertNotificationResultCompletes(granted = true)
    }

    @Test
    fun `notification denial completes permission flow without location denial`() = runTest {
        assertNotificationResultCompletes(granted = false)
    }

    @Test
    fun `previous notification denial is not requested automatically`() = runTest {
        val status = pendingNotificationStatus().copy(wasRequested = true)
        val provider = FakeNotificationPermissionStateProvider(status)
        val vm = buildVm(TestScenario(hasLocationPermission = true), provider)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onContinueClicked(false)

        assertEquals(PermissionUiEvent.Completed, event.await())
        assertEquals(0, provider.markRequestedCount)
    }

    @Test
    fun `existing user receives notification only request`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val vm = buildVm(TestScenario(hasLocationPermission = true), provider)
        vm.configure(PermissionEducationMode.NOTIFICATION_ONLY)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onContinueClicked(false)

        assertEquals(
            PermissionUiEvent.RequestPermission(PermissionRequest.NOTIFICATIONS),
            event.await()
        )
        assertEquals(PermissionEducationMode.NOTIFICATION_ONLY, vm.uiState.value.mode)
    }

    @Test
    fun `request marker is persisted before notification effect`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val vm = buildVm(TestScenario(hasLocationPermission = true), provider)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onContinueClicked(false)

        event.await()
        assertEquals(1, provider.markRequestedCount)
        assertTrue(provider.snapshot().wasRequested)
    }

    @Test
    fun `restored in flight request is not launched twice`() = runTest {
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val savedState = SavedStateHandle(
            mapOf("permission_request_in_flight" to PermissionRequest.NOTIFICATIONS)
        )
        val vm = buildVm(
            scenario = TestScenario(hasLocationPermission = true),
            provider = provider,
            savedStateHandle = savedState
        )

        vm.onContinueClicked(false)
        runCurrent()

        assertEquals(PermissionRequest.NOTIFICATIONS, vm.uiState.value.requestInFlight)
        assertEquals(0, provider.markRequestedCount)
    }

    @Test
    fun `location rationale is shown for location permission only`() {
        val vm = buildVm(
            TestScenario(hasLocationPermission = false),
            FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        )

        vm.onContinueClicked(shouldShowLocationRationale = true)

        assertTrue(vm.uiState.value.showLocationRationaleDialog)
        assertNull(vm.uiState.value.requestInFlight)
    }

    @Test
    fun `acknowledging location denial emits mandatory denial`() = runTest {
        val vm = buildVm(
            TestScenario(hasLocationPermission = false),
            FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        )
        vm.onContinueClicked(false)
        vm.onLocationResult(false)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onLocationDeniedAcknowledged()

        assertEquals(PermissionUiEvent.LocationDenied, event.await())
        assertFalse(vm.uiState.value.showLocationDeniedDialog)
    }

    private fun assertNotificationResultCompletes(granted: Boolean) = runTest {
        val provider = FakeNotificationPermissionStateProvider(pendingNotificationStatus())
        val vm = buildVm(TestScenario(hasLocationPermission = true), provider)
        vm.onContinueClicked(false)
        val event = async { awaitEvent(vm) }
        runCurrent()

        vm.onNotificationResult(granted)

        assertEquals(PermissionUiEvent.Completed, event.await())
        assertFalse(vm.uiState.value.showLocationDeniedDialog)
    }

    private suspend fun awaitEvent(vm: PermissionViewModel): PermissionUiEvent =
        withTimeout(2_000) { vm.events.first() }

    private fun buildVm(
        scenario: TestScenario,
        provider: NotificationPermissionStateProvider,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = PermissionViewModel(
        savedStateHandle = savedStateHandle,
        appChecksProvider = FakeAppChecksProvider { scenario },
        notificationPermissionStateProvider = provider
    )

    private fun pendingNotificationStatus() = NotificationPermissionSnapshot(
        requiresRuntimePermission = true,
        isRuntimePermissionGranted = false,
        wasRequested = false,
        areAppNotificationsEnabled = false,
        isMessageChannelEnabled = true
    )

    private fun preAndroid13Status() = NotificationPermissionSnapshot(
        requiresRuntimePermission = false,
        isRuntimePermissionGranted = true,
        wasRequested = false,
        areAppNotificationsEnabled = true,
        isMessageChannelEnabled = true
    )

    private fun grantedNotificationStatus() = NotificationPermissionSnapshot(
        requiresRuntimePermission = true,
        isRuntimePermissionGranted = true,
        wasRequested = true,
        areAppNotificationsEnabled = true,
        isMessageChannelEnabled = true
    )
}

private class FakeNotificationPermissionStateProvider(
    private var status: NotificationPermissionSnapshot
) : NotificationPermissionStateProvider {
    var markRequestedCount = 0
        private set

    override fun snapshot(): NotificationPermissionSnapshot = status

    override fun markRequested() {
        markRequestedCount += 1
        status = status.copy(wasRequested = true)
    }
}
