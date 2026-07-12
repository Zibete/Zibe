package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.testing.TestScenario

class FakeAppChecksProvider(
    private val scenarioProvider: () -> TestScenario
) : AppChecksProvider {
    override fun hasInternetConnection(): Boolean = scenarioProvider().hasInternet
    override fun hasLocationPermission(): Boolean = scenarioProvider().hasLocationPermission
}
