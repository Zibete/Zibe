package com.zibete.proyecto1.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionCoordinatorTest {
    @Test
    fun `automatic routing never opens notification settings`() {
        assertEquals(
            NotificationPermissionDecision.NONE,
            notificationPermissionDecision(
                status = deniedStatus(wasRequested = true),
                shouldShowRationale = false,
                isExplicitUserAction = false
            )
        )
    }

    @Test
    fun `explicit first request asks for runtime permission`() {
        assertEquals(
            NotificationPermissionDecision.REQUEST_PERMISSION,
            notificationPermissionDecision(
                status = deniedStatus(wasRequested = false),
                shouldShowRationale = false,
                isExplicitUserAction = true
            )
        )
    }

    @Test
    fun `explicit action retries when system shows notification rationale`() {
        assertEquals(
            NotificationPermissionDecision.REQUEST_PERMISSION,
            notificationPermissionDecision(
                status = deniedStatus(wasRequested = true),
                shouldShowRationale = true,
                isExplicitUserAction = true
            )
        )
    }

    @Test
    fun `explicit permanent denial opens notification settings`() {
        assertEquals(
            NotificationPermissionDecision.OPEN_SETTINGS,
            notificationPermissionDecision(
                status = deniedStatus(wasRequested = true),
                shouldShowRationale = false,
                isExplicitUserAction = true
            )
        )
    }

    @Test
    fun `globally disabled notifications open settings even with runtime grant`() {
        assertEquals(
            NotificationPermissionDecision.OPEN_SETTINGS,
            notificationPermissionDecision(
                status = grantedStatus(areAppNotificationsEnabled = false),
                shouldShowRationale = false,
                isExplicitUserAction = true
            )
        )
    }

    @Test
    fun `disabled message channel opens settings`() {
        assertEquals(
            NotificationPermissionDecision.OPEN_SETTINGS,
            notificationPermissionDecision(
                status = grantedStatus(isMessageChannelEnabled = false),
                shouldShowRationale = false,
                isExplicitUserAction = true
            )
        )
    }

    private fun deniedStatus(wasRequested: Boolean) = NotificationPermissionSnapshot(
        requiresRuntimePermission = true,
        isRuntimePermissionGranted = false,
        wasRequested = wasRequested,
        areAppNotificationsEnabled = false,
        isMessageChannelEnabled = true
    )

    private fun grantedStatus(
        areAppNotificationsEnabled: Boolean = true,
        isMessageChannelEnabled: Boolean = true
    ) = NotificationPermissionSnapshot(
        requiresRuntimePermission = true,
        isRuntimePermissionGranted = true,
        wasRequested = true,
        areAppNotificationsEnabled = areAppNotificationsEnabled,
        isMessageChannelEnabled = isMessageChannelEnabled
    )
}
