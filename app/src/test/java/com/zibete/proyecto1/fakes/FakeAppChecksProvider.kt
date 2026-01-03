package com.zibete.proyecto1.fakes

import android.content.Context
import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.testing.UnitScenario

class FakeAppChecksProvider(
    private val scenarioProvider: () -> UnitScenario
) : AppChecksProvider {
    override fun hasInternetConnection(context: Context): Boolean = scenarioProvider().hasInternet
    override fun hasLocationPermission(context: Context): Boolean = scenarioProvider().hasLocationPermission
}
