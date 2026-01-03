package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.SessionBootstrapper

class FakeSessionBootstrapper : SessionBootstrapper {

    override suspend fun bootstrap(
        uid: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit> = ZibeResult.Success(Unit)
}

