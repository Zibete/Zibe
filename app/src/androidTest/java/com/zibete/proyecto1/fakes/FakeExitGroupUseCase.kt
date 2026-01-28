package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import com.zibete.proyecto1.testing.TestScenario

class FakeExitGroupUseCase(
    private val scenarioProvider: () -> TestScenario
) : ExitGroupUseCase {

    override suspend fun performExitGroupDataCleanup(
        message: String
    ): ZibeResult<Unit> {
        return if (scenarioProvider().shouldFail) {
            ZibeResult.Failure(scenarioProvider().runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
    }
}
