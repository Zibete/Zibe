package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.testing.TestScenario

class FakeDeleteAccountUseCase(
    private val scenarioProvider: () -> TestScenario
) : DeleteAccountUseCase {

    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException
    var wasCalled: Boolean = false

    override suspend fun execute(): ZibeResult<Unit> {
        wasCalled = true
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success()
        }
    }
}
