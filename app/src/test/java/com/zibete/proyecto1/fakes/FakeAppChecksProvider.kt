package com.zibete.proyecto1.fakes

import android.content.Context
import com.zibete.proyecto1.core.utils.AppChecksProvider

class FakeAppChecksProvider(
    var internet: Boolean = true,
    var locationPermission: Boolean = true
) : AppChecksProvider {
    override fun hasInternetConnection(context: Context) = internet
    override fun hasLocationPermission(context: Context) = locationPermission
}
