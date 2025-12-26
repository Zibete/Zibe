package com.zibete.proyecto1.fakes

import android.content.Intent
import com.zibete.proyecto1.domain.session.LogoutUseCase

class FakeLogoutUseCase(
    private val intentToReturn: Intent = Intent()
) : LogoutUseCase {

    override suspend fun execute(): Intent = intentToReturn
}
