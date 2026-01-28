package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.testing.TestScenario

class FakeLogoutUseCase(
    private val scenarioProvider: () -> TestScenario
) : LogoutUseCase {

    override suspend fun execute(): ZibeResult<Unit> =
        if (scenarioProvider().shouldFail) {
            ZibeResult.Failure(scenarioProvider().runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
}
