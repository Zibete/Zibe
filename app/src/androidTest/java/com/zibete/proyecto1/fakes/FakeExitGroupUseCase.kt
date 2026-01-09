package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.constants.EXIT_GROUP_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import javax.inject.Inject

class FakeExitGroupUseCase @Inject constructor(
) : ExitGroupUseCase {

    var shouldFail: Boolean = false
    var failure: Throwable = IllegalStateException(EXIT_GROUP_ERR_EXCEPTION)

    override suspend fun performExitGroupDataCleanup(
        message: String
    ): ZibeResult<Unit> {
        return if (shouldFail) {
            ZibeResult.Failure(failure)
        } else {
            ZibeResult.Success(Unit)
        }
    }
}