package com.zibete.proyecto1.fakes

import android.content.Intent
import com.zibete.proyecto1.domain.session.LogoutOrchestrator

class FakeLogoutOrchestrator(
    private val intentToReturn: Intent = Intent()
) : LogoutOrchestrator {

    override suspend fun execute(): Intent = intentToReturn
}
