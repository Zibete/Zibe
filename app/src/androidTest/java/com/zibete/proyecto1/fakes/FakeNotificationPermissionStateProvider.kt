package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.notifications.NotificationPermissionSnapshot
import com.zibete.proyecto1.notifications.NotificationPermissionStateProvider
import com.zibete.proyecto1.testing.TestScenario

class FakeNotificationPermissionStateProvider(
    private val scenarioProvider: () -> TestScenario
) : NotificationPermissionStateProvider {
    override fun snapshot(): NotificationPermissionSnapshot {
        val scenario = scenarioProvider()
        return NotificationPermissionSnapshot(
            requiresRuntimePermission = scenario.notificationRuntimeRequired,
            isRuntimePermissionGranted = scenario.notificationPermissionGranted,
            wasRequested = scenario.notificationWasRequested,
            areAppNotificationsEnabled = scenario.systemNotificationsEnabled,
            isMessageChannelEnabled = scenario.messageChannelEnabled
        )
    }

    override fun markRequested() {
        scenarioProvider().notificationWasRequested = true
    }
}
