package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.testing.UnitScenario

class FakeLogoutUseCase(
    private val scenarioProvider: () -> UnitScenario
) : LogoutUseCase {

    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    override suspend fun execute(): ZibeResult<Unit> =
        if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
}
