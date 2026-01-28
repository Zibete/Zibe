package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.testing.TestScenario

class FakeSessionBootstrapper(
    private val scenarioProvider: () -> TestScenario
) : SessionBootstrapper {

    override suspend fun bootstrap(
        uid: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit> {
        return if (scenarioProvider().shouldFail) {
            ZibeResult.Failure(scenarioProvider().runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
    }
}
