package com.zibete.proyecto1.notifications

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionCoordinatorTest {

    @Test
    fun `pre Android 13 never requests runtime notification permission`() {
        assertEquals(
            NotificationPermissionDecision.NONE,
            notificationPermissionDecision(32, false, false, false)
        )
    }

    @Test
    fun `first request explains without blocking app usage`() {
        assertEquals(
            NotificationPermissionDecision.EXPLAIN_AND_REQUEST,
            notificationPermissionDecision(
                Build.VERSION_CODES.TIRAMISU,
                isGranted = false,
                wasRequested = false,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `permanent denial routes to app settings`() {
        assertEquals(
            NotificationPermissionDecision.OPEN_SETTINGS,
            notificationPermissionDecision(
                Build.VERSION_CODES.TIRAMISU,
                isGranted = false,
                wasRequested = true,
                shouldShowRationale = false
            )
        )
    }
}
