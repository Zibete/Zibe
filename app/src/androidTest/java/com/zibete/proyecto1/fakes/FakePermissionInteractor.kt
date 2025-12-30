package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.ui.custompermission.di.PermissionInteractor

class FakePermissionInteractor(
    private val rationale: Boolean,
    private val granted: Boolean
) : PermissionInteractor {

    override fun shouldShowRationale(): Boolean = rationale

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        onResult(granted)
    }
}
