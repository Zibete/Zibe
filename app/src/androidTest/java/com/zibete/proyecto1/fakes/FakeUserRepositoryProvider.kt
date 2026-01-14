package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestData.RUNTIME_EXCEPTION

class FakeUserRepositoryProvider(
    var shouldFail: Boolean = false,
    val runtimeException: Throwable = RuntimeException(RUNTIME_EXCEPTION)
) : UserRepositoryProvider {

    override suspend fun accountExists(uid: String): Boolean =
        !shouldFail

    override suspend fun hasBirthDate(uid: String): Boolean =
        !shouldFail

    override suspend fun getProfilePhotoUrl(): String? =
        if (!shouldFail) TestData.PHOTO_URL else null

    override suspend fun getAccount(uid: String): Users? {
        if (shouldFail) throw runtimeException
        return TestData.USER
    }

    override suspend fun getChatStateWith(
        otherUid: String,
        nodeType: String
    ): String {
        if (shouldFail) throw runtimeException
        return TestData.CHAT_STATE
    }

    override suspend fun getMyAccount(): Users? {
        TODO("Not yet implemented")
    }

}
