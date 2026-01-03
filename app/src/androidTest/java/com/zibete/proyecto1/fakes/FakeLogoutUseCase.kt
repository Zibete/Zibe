package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.testing.TestData.RUNTIME_EXCEPTION

class FakeLogoutUseCase(
    var shouldFail: Boolean = false,
    val runtimeException: Throwable = RuntimeException(RUNTIME_EXCEPTION)
) : LogoutUseCase {

    override suspend fun execute(): ZibeResult<Unit> =
        if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }
}