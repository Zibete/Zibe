package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.domain.session.SessionBootstrapper

class FakeSessionBootstrapper : SessionBootstrapper {

    var calledWithUid: String? = null

    override suspend fun bootstrap(uid: String) {
        calledWithUid = uid
        // no-op
    }
}

