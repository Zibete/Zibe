package com.zibete.proyecto1.fakes

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.testing.TestScenario

class FakeUserRepositoryActions(
    private val scenarioProvider: () -> TestScenario
) : UserRepositoryActions {

    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    override suspend fun createUserNode(
        firebaseUser: FirebaseUser,
        name: String,
        birthDate: String,
        description: String
    ) {
        if (shouldFail) throw runtimeException
    }

    override suspend fun setUserLastSeen() {
        if (shouldFail) throw runtimeException
    }

    override suspend fun setUserActivityStatus(status: String) {
        if (shouldFail) throw runtimeException
    }

    override suspend fun deleteMyAccountData(): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun deleteProfilePhoto(): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun putProfilePhotoInStorage(localUri: Uri): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)

    override suspend fun updateUserFields(fields: Map<String, Any?>) {
        if (shouldFail) throw runtimeException
    }

    override suspend fun updateLocalProfile(name: String?, photoUrl: String?, email: String?) {
        if (shouldFail) throw runtimeException
    }

    override suspend fun sendFeedback(
        feedback: String,
        screen: String,
        model: String,
        appVersion: String
    ): ZibeResult<Unit> =
        if (shouldFail) ZibeResult.Failure(runtimeException) else ZibeResult.Success(Unit)
}
