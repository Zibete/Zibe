package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.ZibeResult
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase

class FakeDeleteAccountUseCase(
) : DeleteAccountUseCase {
    var called: Boolean = false

    override suspend fun execute(): ZibeResult<Unit> {
        called = true
        return ZibeResult.Success(Unit)
    }

}
