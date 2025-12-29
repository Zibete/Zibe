package com.zibete.proyecto1.testing.fakes

import com.zibete.proyecto1.domain.session.LogoutUseCase

class FakeLogoutUseCase(
) : LogoutUseCase {

    override suspend fun execute() {}
}