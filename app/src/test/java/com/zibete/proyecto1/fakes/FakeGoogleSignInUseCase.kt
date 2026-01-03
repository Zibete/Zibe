package com.zibete.proyecto1.fakes

import android.app.Activity
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.testing.TestData.GOOGLE_ID_TOKEN
import com.zibete.proyecto1.testing.UnitScenario

class FakeGoogleSignInUseCase (
    private val scenarioProvider: () -> UnitScenario
): GoogleSignInUseCase {
    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    override suspend operator fun invoke(activity: Activity): ZibeResult<String> =
        if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(GOOGLE_ID_TOKEN)
        }

}
