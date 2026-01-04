package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.testing.TestScenario

class FakeSessionBootstrapper(
    private val scenarioProvider: () -> TestScenario
) : SessionBootstrapper {
    var wasCalled: Boolean = false
    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    override suspend fun bootstrap(
        uid: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit> {
        wasCalled = true
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
    }
}

