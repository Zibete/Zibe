package com.zibete.proyecto1.testing.fakes

import android.content.Context
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.utils.AppChecksProvider

class FakeAppChecksProvider(
    private val scenario: TestScenario
) : AppChecksProvider {

    override fun hasInternetConnection(context: Context): Boolean = scenario.hasInternet
    override fun hasLocationPermission(context: Context): Boolean = scenario.hasLocationPermission
}
