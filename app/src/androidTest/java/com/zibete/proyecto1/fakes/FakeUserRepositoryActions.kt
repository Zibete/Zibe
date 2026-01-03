package com.zibete.proyecto1.fakes

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.testing.TestData.RUNTIME_EXCEPTION

class FakeUserRepositoryActions (
    var shouldFail: Boolean = false,
    val runtimeException: Throwable = RuntimeException(RUNTIME_EXCEPTION),
): UserRepositoryActions {

    override suspend fun createUserNode(firebaseUser: FirebaseUser, birthDate: String, description: String) {
        if (shouldFail) {
            throw runtimeException
        }
    }

    override suspend fun setUserLastSeen() {
        if (shouldFail) {
            throw runtimeException
        }
    }

    override suspend fun setUserActivityStatus(status: String) {
        if (shouldFail) {
            throw runtimeException
        }
    }

    override suspend fun deleteMyAccountData(): ZibeResult<Unit> =
        if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }


    override suspend fun deleteProfilePhoto() {
        if (shouldFail) {
            throw runtimeException
        }
    }

    override suspend fun putProfilePhotoInStorage(localUri: Uri) {
        if (shouldFail) {
            throw runtimeException
        }
    }

    override suspend fun updateUserFields(fields: Map<String, Any?>) {
        if (shouldFail) {
            throw runtimeException
        }
    }
}

