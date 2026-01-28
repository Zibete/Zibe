package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.testing.TestScenario

class FakeDeleteAccountUseCase(
    private val scenarioProvider: () -> TestScenario
) : DeleteAccountUseCase {
    var wasCalled: Boolean = false

    override suspend fun execute(): ZibeResult<Unit> {
        wasCalled = true
        return if (scenarioProvider().shouldFail) {
            ZibeResult.Failure(scenarioProvider().runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
    }
}
