package com.zibete.proyecto1.testing.fakes

import com.zibete.proyecto1.domain.session.DeleteAccountResult
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase

class FakeDeleteAccountUseCase(
    var result: DeleteAccountResult = DeleteAccountResult.Success
) : DeleteAccountUseCase {

    var called: Boolean = false

    override suspend fun execute(): DeleteAccountResult {
        called = true
        return result
    }
}
