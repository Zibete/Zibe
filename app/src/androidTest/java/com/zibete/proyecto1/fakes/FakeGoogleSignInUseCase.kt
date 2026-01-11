package com.zibete.proyecto1.fakes

import android.app.Activity
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase


class FakeGoogleSignInUseCase (
): GoogleSignInUseCase {
    var shouldFail: Boolean = false
    var failure: Throwable = IllegalStateException("Google Sign-In failed")
    var returnedIdToken: String = "fake-google-id-token"

    override suspend operator fun invoke(activity: Activity): ZibeResult<String> {
        return if (shouldFail) {
            ZibeResult.Failure(failure)
        } else {
            ZibeResult.Success(returnedIdToken)
        }
    }
}
