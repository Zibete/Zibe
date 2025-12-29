package com.zibete.proyecto1.testing.fakes

import com.zibete.proyecto1.domain.session.SessionBootstrapper

class FakeSessionBootstrapper : SessionBootstrapper {
    override suspend fun bootstrap(uid: String) = Unit
}

